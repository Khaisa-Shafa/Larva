package com.dicoding.skripsiapp.fragment.main.bookmarked.detailbookmark

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.data.ClassificationReport
import com.dicoding.skripsiapp.data.ConfusionMatrix
import com.dicoding.skripsiapp.data.ModelResults
import com.dicoding.skripsiapp.databinding.FragmentDetailBookmarkBinding
import com.dicoding.skripsiapp.util.DialogUtilsClassification
import com.dicoding.skripsiapp.util.DialogUtilsClassification.showClassificationReportDialog
import com.dicoding.skripsiapp.util.DialogUtilsClassification.showConfusionMatrixDialog
import com.dicoding.skripsiapp.util.DialogUtilsClassification.showModelResultsDialog
import com.dicoding.skripsiapp.util.DialogUtilsPrediction
import com.dicoding.skripsiapp.util.JsonUtils
import com.dicoding.skripsiapp.util.hideBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.DetailBookmarkViewModel
import com.dicoding.skripsiapp.viewmodel.PageClassificationViewModel
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class DetailBookmarkFragment : Fragment() {

    private var _binding: FragmentDetailBookmarkBinding? = null
    private val binding get() = _binding!!

    private val bookmarkDetailViewModel: DetailBookmarkViewModel by viewModels()

    private val pageClassificationViewModel by viewModels<PageClassificationViewModel>()

    private val pageDetectionViewModel by viewModels<PageDetectionViewModel>()

    private val args: DetailBookmarkFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        hideBottomNavigationView()
        _binding = FragmentDetailBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookmarkId = args.bookmarkId
        if (bookmarkDetailViewModel.bookmarkItem.value == null) {
            bookmarkDetailViewModel.fetchBookmark(bookmarkId)
        }

        setupWindowInsets()
        setupUI()
        setupObservers()
    }

    private fun setupWindowInsets() {
        val pageClassificationView = view?.findViewById<View>(R.id.fragment_detail)
        if (pageClassificationView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(pageClassificationView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        } else {
            Log.e("PageClassificationFragment", "View with ID 'main' not found")
        }
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnLoadImage.isEnabled = false
        binding.btnLoadImage.setOnClickListener {
            showToast("Action tidak tersedia di halaman ini!")
        }

        binding.btnCaptureImage.isEnabled = false
        binding.btnCaptureImage.setOnClickListener {
            showToast("Action tidak tersedia di halaman ini!")
        }

        binding.btnResetImage.isEnabled = false
        binding.btnResetImage.setOnClickListener {
            showToast("Action tidak tersedia di halaman ini!")
        }

        binding.ivBookmark.setOnClickListener { handleBookmarkClick() }

        binding.ivMore.setOnClickListener {
            bookmarkDetailViewModel.bookmarkItem.value?.let { showPopupMenu(it.type) }
        }

        binding.ivLocation.setOnClickListener {
            bookmarkDetailViewModel.bookmarkItem.value?.let { bookmark ->
                Log.d("BookmarkLocation", "Latitude: ${bookmark.latitude}, Longitude: ${bookmark.longitude}") // Log nilai latitude dan longitude
                openGoogleMapsWeb(bookmark.latitude, bookmark.longitude)
            } ?: showToast("Lokasi tidak tersedia")
        }


    }

    private fun openGoogleMapsWeb(latitude: Double, longitude: Double) {
        val uri = Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
        Log.d("GoogleMapsURI", "URI: $uri")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Flag untuk membuka aktivitas baru
        val availableActivity = intent.resolveActivity(requireActivity().packageManager)
        if (availableActivity != null) {
            startActivity(intent)
        } else {
            // Coba membuka URL di browser jika Google Maps tidak tersedia
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=$latitude,$longitude"))
            startActivity(browserIntent)
        }

    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bookmarkDetailViewModel.isLoading.collectLatest { isLoading ->
                        binding.progressbar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.imageView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE

                    }
                }
                launch {
                    bookmarkDetailViewModel.bookmarkItem.collectLatest { item ->
                        item?.let {
                            updateBookmarkDetail(it)
                        }
                    }
                }
                launch {
                    bookmarkDetailViewModel.isBookmarked.collectLatest { isBookmarked ->
                        updateBookmarkIcon(isBookmarked)
                    }
                }
            }
        }
    }

    private fun updateBookmarkDetail(item: BookmarkItem) {
        binding.txtTitle.text =
            if (item.type == "Classification") "Detail Classification" else "Detail Detection"

        val imageUri = if (item.imageUri.isNotEmpty()) Uri.parse(item.imageUri)
        else Uri.parse("android.resource://${requireContext().packageName}/${R.drawable.ic_placeholder_image}")

        showLoading()
        binding.imageView.visibility = View.INVISIBLE

        Glide.with(this)
            .load(imageUri)
            .placeholder(R.drawable.ic_placeholder_image)
            .error(R.drawable.ic_placeholder_image)
            .listener(object: RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    hideLoading()
                    binding.imageView.visibility = View.VISIBLE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    hideLoading()
                    binding.imageView.visibility = View.VISIBLE
                    return false
                }
            })
            .into(binding.imageView)

        binding.tvOutput.text = item.fullClassificationResults
    }

    private fun handleBookmarkClick() {
        binding.ivBookmark.isEnabled = false // Prevent multiple clicks
        showLoading()

        val currentBookmark = bookmarkDetailViewModel.bookmarkItem.value
        if (bookmarkDetailViewModel.isBookmarked.value) {
            bookmarkDetailViewModel.removeBookmark()
            showToast("Bookmark dihapus")
        } else {
            currentBookmark?.let { bookmark ->
                val newBookmark = bookmark.copy(
                    id = UUID.randomUUID().toString(),
                    isBookmarked = true,
                    date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                bookmarkDetailViewModel.addBookmark(newBookmark)
                showToast("Bookmark ditambahkan")
            } ?: showToast("Error: Bookmark tidak ditemukan!")
        }

        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000) // Prevent spam clicking
            binding.ivBookmark.isEnabled = true
            hideLoading()
        }
    }

    private fun updateBookmarkIcon(isBookmarked: Boolean) {
        binding.ivBookmark.setImageResource(
            if (isBookmarked) R.drawable.ic_bookmarked_filled else R.drawable.ic_bookmarked
        )
    }

    private fun showPopupMenu(type: String){
        val popupMenu = PopupMenu(requireContext(), binding.ivMore)
        val menuInflater = popupMenu.menuInflater
        val menu = popupMenu.menu

        if (type == "Classification") {
            menuInflater.inflate(R.menu.popup_menu_classification, menu)
        } else if (type == "Detection") {
            menuInflater.inflate(R.menu.popup_menu_detection, menu)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (type) {
                "Classification" -> {
                    when (menuItem.itemId) {
                        R.id.menuClassificationReport -> {
                            showMenuClassificationReport()
                            true
                        }
                        R.id.menuConfusionMatrix -> {
                            showMenuConfusionMatrix()
                            true
                        }
                        R.id.menuModelResult -> {
                            showModelResult()
                            true
                        }
                        else -> false
                    }
                }
                "Detection" -> {
                    when (menuItem.itemId) {
                        R.id.menuConfusionMatrix -> {
                            val (confusionMatrix, classes) = pageDetectionViewModel.loadConfusionMatrix(requireContext())
                            DialogUtilsPrediction.showConfusionMatrixDialog(requireContext(), confusionMatrix, classes)
                            true
                        }
                        R.id.menuModelResult -> {
                            val trainingMetrics = pageDetectionViewModel.loadTrainingMetrics(requireContext())
                            DialogUtilsPrediction.showTrainingMetricsTableDialog(requireContext(), trainingMetrics)
                            true
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showMenuClassificationReport() {
        readJsonAndShowDialog("classification_report.json", ClassificationReport::class.java) {
            showClassificationReportDialog(requireContext(), it)
        }
    }

    private fun showMenuConfusionMatrix() {
        readJsonAndShowDialog("confusion_matrix.json", ConfusionMatrix::class.java) {
            it.confusionMatrix?.let { matrix ->
                showConfusionMatrixDialog(requireContext(), matrix, it.classes)
            }
        }
    }

    private fun showModelResult() {
        readJsonAndShowDialog("model_results.json", ModelResults::class.java) {
            showModelResultsDialog(requireContext(), it)
        }
    }

    private fun <T> readJsonAndShowDialog(fileName: String, clazz: Class<T>, showDialog: (T) -> Unit) {
        JsonUtils.readJsonFromAssets(requireContext(), fileName, clazz)?.let { showDialog(it) }
            ?: Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideLoading() {
        binding.progressbar.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressbar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}