package com.dicoding.skripsiapp.fragment.main.profile

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.activity.LoginRegisterActivity
import com.dicoding.skripsiapp.data.User
import com.dicoding.skripsiapp.databinding.FragmentProfileBinding
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.FavoriteViewModel
import com.dicoding.skripsiapp.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    val viewModel by viewModels<ProfileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentProfile) { view, insets ->
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

        binding.btnLogoutProfile.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.logout()
                    val intent = Intent(requireActivity(), LoginRegisterActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.linearEdtProfile.setOnClickListener {
            animateButtonClick(binding.linearEdtProfile)
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.linearEdtSettings.setOnClickListener {
            animateButtonClick(binding.linearEdtSettings)
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment2)
        }

        binding.linearEdtAbout.setOnClickListener {
            animateButtonClick(binding.linearEdtAbout)
            findNavController().navigate(R.id.action_profileFragment_to_aboutFragment2)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    viewModel.user.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                binding.progressbarSettings.visibility = View.VISIBLE
                            }
                            is Resource.Success -> {
                                binding.progressbarSettings.visibility = View.GONE
                                val user = it.data
                                updateUserUI(user)
                            }
                            is Resource.Error -> {
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                                binding.progressbarSettings.visibility = View.GONE

                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun updateUserUI(user: User?) {
        if (user == null) {
            Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            return
        }

        // Mengisi gambar profil
        user.imagePath.takeIf { it.isNotEmpty() }?.let {
            Glide.with(requireView())
                .load(it)
                .error(ColorDrawable(Color.BLACK))
                .fallback(R.drawable.image_profile)
                .into(binding.ivImageUser)
        } ?: run {
            Glide.with(requireView())
                .load(R.drawable.image_profile)
                .into(binding.ivImageUser)
        }

        // Menampilkan data pengguna
        binding.apply {
            tvUserName.text = "${user.firstName} ${user.lastName}"
            tvEmail.text = user.email
            tvEmailProfile.text = user.email
            tvFirstNameProfile.text = user.firstName
            tvLastNameProfile.text = user.lastName
        }
    }

    private fun animateButtonClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
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