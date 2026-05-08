package com.dicoding.skripsiapp.fragment.main.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.User
import com.dicoding.skripsiapp.databinding.FragmentEditProfileBinding
import com.dicoding.skripsiapp.dialog.setupBottomSheetDialog
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.EditProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<EditProfileViewModel>()

    private lateinit var imageActivityResultLauncher: ActivityResultLauncher<Intent>

    private var imageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value } // Cek apakah semua izin diberikan
            if (allGranted) {
                openGallery()
            } else {
                val shouldShowRationale = permissions.keys.any { permission ->
                    shouldShowRequestPermissionRationale(permission)
                }
                if (!shouldShowRationale) {
                    // Jika izin ditolak permanen
                    Toast.makeText(
                        requireContext(),
                        "Permission permanently denied. Please enable it in app settings.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Jika izin hanya ditolak sementara
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    viewModel.setSelectedImageUri(imageUri)
                    Glide.with(this).load(imageUri).into(binding.imageUser)
                }
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentEditProfile) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0) // Tambahkan inset atas
            insets
        }

        // Periksa orientasi perangkat
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Jika orientasi horizontal (landscape), set fitsSystemWindows = true
            ViewCompat.setFitsSystemWindows(binding.root, true)
        } else {
            // Jika orientasi vertikal (portrait), set fitsSystemWindows = false
            ViewCompat.setFitsSystemWindows(binding.root, false)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    viewModel.user.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                showUserLoading()
                            }
                            is Resource.Success -> {
                                hideUserLoading()
                                showUserInformation(it.data!!)
                            }
                            is Resource.Error -> {
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.updateInfo.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                binding.buttonSave.startAnimation()
                            }
                            is Resource.Success -> {
                                binding.buttonSave.revertAnimation()
                                findNavController().navigateUp()
                            }
                            is Resource.Error -> {
                                binding.buttonSave.revertAnimation()
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.resetPassword.collect {
                        when (it) {
                            is Resource.Loading -> {

                            }
                            is Resource.Success -> {
                                Snackbar.make(requireView(), "Reset link was sent to your email", Snackbar.LENGTH_LONG).show()
                            }
                            is Resource.Error -> {
                                Snackbar.make(requireView(), "Error: ${it.message}", Snackbar.LENGTH_LONG).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.selectedImageUri.collectLatest { uri ->
                        if (uri != null) {
                            Glide.with(this@EditProfileFragment)
                                .load(uri)
                                .into(binding.imageUser)
                        }
                    }
                }
            }
        }

        binding.imageCloseUserAccount.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvUpdatePassword.setOnClickListener {
            setupBottomSheetDialog { email ->
                viewModel.resetPassword(email)
            }
        }

        binding.buttonSave.setOnClickListener {
            binding.apply {
                val firstName = edFirstName.text.toString().trim()
                val lastName = edLastName.text.toString().trim()
                val email = edEmail.text.toString().trim()
                val user = User(firstName, lastName, email)
                viewModel.updateUser(user, viewModel.selectedImageUri.value)
            }
        }

        binding.imageEdit.setOnClickListener{
            checkPermissionsAndOpenGallery()
        }
    }

    private fun checkPermissionsAndOpenGallery() {
        val requiredPermissions = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                // Untuk Android 13 dan lebih baru
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                // Untuk Android 10 hingga Android 12
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Untuk Android 9 dan sebelumnya
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        // Filter izin yang belum diberikan
        val deniedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        // Jika semua izin diberikan, buka galeri
        if (deniedPermissions.isEmpty()) {
            openGallery()
        } else {
            // Minta izin
            requestPermissionLauncher.launch(deniedPermissions.toTypedArray())
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        imageActivityResultLauncher.launch(intent)
    }

    // Di EditProfileFragment dan ProfileFragment
    fun loadProfileImage(imageView: ImageView, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.image_profile)
            return
        }

        if (imagePath.startsWith("data:image")) {
            // Base64
            val base64 = imagePath.substringAfter("base64,")
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bitmap)
        } else {
            // URL biasa (lama)
            Glide.with(imageView.context).load(imagePath).into(imageView)
        }
    }


    private fun showUserInformation(data: User) {
        binding.apply {
            Glide.with(this@EditProfileFragment)
                .load(data.imagePath)
                .error(ColorDrawable(Color.BLACK))
                .into(imageUser)
            edFirstName.setText(data.firstName)
            edLastName.setText(data.lastName)
            edEmail.setText(data.email)
        }
    }

    private fun hideUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.GONE
            imageUser.visibility = View.VISIBLE
            imageEdit.visibility = View.VISIBLE
            edFirstName.visibility = View.VISIBLE
            edLastName.visibility = View.VISIBLE
            edEmail.visibility = View.VISIBLE
            buttonSave.visibility = View.VISIBLE
        }
    }

    private fun showUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.VISIBLE
            imageUser.visibility = View.INVISIBLE
            imageEdit.visibility = View.INVISIBLE
            edFirstName.visibility = View.INVISIBLE
            edLastName.visibility = View.INVISIBLE
            edEmail.visibility = View.INVISIBLE
            buttonSave.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}