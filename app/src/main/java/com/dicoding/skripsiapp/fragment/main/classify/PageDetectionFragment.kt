package com.dicoding.skripsiapp.fragment.main.classify

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.activity.LiveDetectionActivity
import com.dicoding.skripsiapp.databinding.FragmentPageDetectionBinding
import com.dicoding.skripsiapp.util.CameraHelper
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.DialogUtilsPrediction
import com.dicoding.skripsiapp.util.ImageUtils
import com.dicoding.skripsiapp.util.ImageUtils.correctBitmapOrientation
import com.dicoding.skripsiapp.util.hideBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PageDetectionFragment : Fragment() {

    private var _binding: FragmentPageDetectionBinding? = null
    private val binding get() = _binding!!

    private var detector: Detector? = null
    private var cameraImageUri: Uri? = null
    private lateinit var pageDetectionObserver: PageDetectionObserver
    private val pageDetectionViewModel by viewModels<PageDetectionViewModel>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bookmarkHandler: BookmarkHandler
    private lateinit var cameraHelper: CameraHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
// Inflate the layout for this fragment
        hideBottomNavigationView()
        _binding = FragmentPageDetectionBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading(true)

        detector?.setup()

        bookmarkHandler = BookmarkHandler(
            this,
            pageDetectionViewModel,
            binding,
            fusedLocationClient
        )

        setupWindowInsets()
        pageDetectionObserver = PageDetectionObserver(
            viewLifecycleOwner,
            pageDetectionViewModel,
            binding,
            ::showLoading,
            ::showToast
        )
        pageDetectionObserver.setupObservers()
        setupUI()
        setupCameraHelper()

        showLoading(false)

    }

    private fun setupWindowInsets() {
        val pageDetectionView = view?.findViewById<View>(R.id.fragment_detection)
        if (pageDetectionView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(pageDetectionView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        } else {
            Log.e("PageDetectionFragment", "View with ID 'main' not found")
        }
    }


    private fun setupUI() {
        binding.apply {
            btnDoDetectino.setOnClickListener {
                selectImageFromGallery()
            }

            ivMore.setOnClickListener {
                val popupMenu = PopupMenu(requireContext(), binding.ivMore)
                popupMenu.menuInflater.inflate(R.menu.popup_menu_detection, popupMenu.menu)

                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.menuConfusionMatrix -> {
                            val (confusionMatrix, classes) = pageDetectionViewModel.loadConfusionMatrix(
                                requireContext()
                            )
                            DialogUtilsPrediction.showConfusionMatrixDialog(
                                requireContext(),
                                confusionMatrix,
                                classes
                            )
                            true
                        }

                        R.id.menuModelResult -> {
                            val trainingMetrics =
                                pageDetectionViewModel.loadTrainingMetrics(requireContext())
                            DialogUtilsPrediction.showTrainingMetricsTableDialog(
                                requireContext(),
                                trainingMetrics
                            )
                            true
                        }

                        else -> false
                    }
                }
                popupMenu.show()
            }

            tvFunfact.setOnClickListener {
                val topClass = pageDetectionViewModel.getTopPredictionClass()
                if (topClass != null) {
                    pageDetectionViewModel.fetchFunFact(topClass) // Fun Fact akan diperbarui melalui observer
                    showToast("Fetching fun fact for $topClass...")
                } else {
                    showToast("No class detected yet.")
                }
            }

            ivBack.setOnClickListener {
                findNavController().navigateUp()
            }
            ivBookmark.setOnClickListener {
                bookmarkHandler.handleBookmark()
            }

            btnResetImage.setOnClickListener {
                if (binding.imageVieww.tag == null) {
                    showToast("Gambar belum diupload")
                } else {
                    showToast("Gambar berhasil di reset")
                    binding.ivBookmark.setImageResource(R.drawable.ic_bookmarked)
                    binding.imageVieww.setImageResource(R.drawable.logo_detection)
                    binding.imageVieww.tag = null
                    cameraImageUri = null
                    binding.tvPrediction.text = ""
                    pageDetectionViewModel.clearImageData()
                }
            }

        }
    }

    private fun selectImageFromGallery() {
        selectImageLauncher.launch("image/*")
    }

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                requireContext().contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (validateBitmap(bitmap)) {
                        binding.imageVieww.setImageBitmap(bitmap)
                        binding.imageVieww.tag = "uploaded"
                        binding.ivBookmark.setImageResource(R.drawable.ic_bookmarked)
                        pageDetectionViewModel.processImage(
                            bitmap,
                            uri,
                            requireContext().contentResolver
                        )
                    }
                }
            }
        }

    private fun checkAndRequestCameraPermission() {
        val cameraPermission = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            captureImageFromCamera()
        } else {
            requestCameraPermissionLauncher.launch(cameraPermission)
        }
    }

    private fun captureImageFromCamera() {
        try {
            cameraHelper.captureImageFromCamera() // Gunakan CameraHelper untuk menangani pengambilan gambar
        } catch (e: Exception) {
            Log.e("PredictionActivity", "Error capturing image: ${e.message}", e)
            showToast("Error capturing image. Please try again.")
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                captureImageFromCamera()
            } else {
                showToast("Camera permission is required")
            }
        }

    private fun validateBitmap(bitmap: Bitmap?): Boolean {
        return ImageUtils.validateBitmap(bitmap)
    }

    private fun setupCameraHelper() {
        cameraHelper = CameraHelper(this).apply {
            onImageCaptured = { uri ->
                uri?.let {
                    cameraImageUri = it
                    requireContext().contentResolver.openInputStream(it)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val correctedBitmap =
                            correctBitmapOrientation(requireContext(), bitmap, uri)
                        binding.imageVieww.setImageBitmap(correctedBitmap)
                        binding.ivBookmark.setImageResource(R.drawable.ic_bookmarked)
                        pageDetectionViewModel.processImage(
                            correctedBitmap,
                            uri,
                            requireContext().contentResolver
                        )
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressbar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        pageDetectionViewModel.fetchBookmarks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}