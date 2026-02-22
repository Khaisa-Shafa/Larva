package com.dicoding.skripsiapp.fragment.loginRegister

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.User
import com.dicoding.skripsiapp.databinding.FragmentRegisterBinding
import com.dicoding.skripsiapp.util.Constants.TAG_REGISTER
import com.dicoding.skripsiapp.util.RegisterValidation
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.RegisterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<RegisterViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentRegister) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarInsets.top, 0, systemBarInsets.bottom) // Tambahkan inset atas
            insets
        }

        binding.tvHaveAccount.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        val constraintLayout = binding.root.findViewById<ConstraintLayout>(R.id.constraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            constraintSet.connect(
                R.id.textView4, ConstraintSet.START,
                R.id.guidelineLoginLeft, ConstraintSet.START
            )
            constraintSet.setMargin(R.id.textView4, ConstraintSet.START, 8)

            constraintSet.connect(
                R.id.imageView3, ConstraintSet.START,
                R.id.guidelineLoginLeft, ConstraintSet.START
            )

            constraintSet.connect(
                R.id.tv_have_account, ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM
            )
            constraintSet.setMargin(R.id.tv_have_account, ConstraintSet.BOTTOM, 16)
        } else {
            constraintSet.connect(
                R.id.textView4, ConstraintSet.START,
                R.id.guidelineLoginLeft, ConstraintSet.START
            )
        }

        constraintSet.applyTo(constraintLayout)

        binding.apply {
            btnRegister.setOnClickListener {
                val user = User(
                    edtFirstName.text.toString().trim(),
                    edtLastName.text.toString().trim(),
                    edtEmail.text.toString().trim()
                )
                val password = edtPassword.text.toString()
                val rePassword = edtRePassword.text.toString()
                viewModel.createAccountWithEmailAndPassword(user, password, rePassword)
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //Collecet register state
                launch {
                    viewModel.register.collect {
                        when(it) {
                            is Resource.Loading -> {
                                binding.btnRegister.startAnimation()
                            }
                            is Resource.Success -> {
                                Log.d("onSuccess: ", it.data.toString())
                                binding.btnRegister.revertAnimation()

                                showSuccessRegisterDialog()
                            }
                            is Resource.Error -> {
                                Log.d(TAG_REGISTER, it.message.toString())
                                binding.btnRegister.revertAnimation()
                                if (it.message == "Email Sudah Terdaftar") {
                                    binding.edtEmail.apply {
                                        error = it.message
                                        requestFocus()
                                    }
                                } else {
                                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> Unit
                        }
                    }
                }

                //collect validation state
                launch {
                    viewModel.validation.collect { validation ->
                        if (validation.email is RegisterValidation.Failed) {
                            withContext(Dispatchers.Main) {
                                binding.edtEmail.apply {
                                    requestFocus()
                                    error = validation.email.message
                                }
                            }
                        }

                        if (validation.password is RegisterValidation.Failed) {
                            withContext(Dispatchers.Main) {
                                binding.edtPassword.apply {
                                    requestFocus()
                                    error = validation.password.message
                                }
                            }
                        }

                        if (validation.rePassword is RegisterValidation.Failed) {
                            withContext(Dispatchers.Main) {
                                binding.edtRePassword.apply {
                                    requestFocus()
                                    error = validation.rePassword.message
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showSuccessRegisterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ Registrasi Berhasil")
            .setMessage("Anda telah terdaftar dan masuk sebagai pengguna baru. Nikmati pengalaman aplikasi kami!")
            .setPositiveButton("Login Sekarang") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }
            .setCancelable(false)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}