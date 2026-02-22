package com.dicoding.skripsiapp.fragment.loginRegister

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
import com.dicoding.skripsiapp.activity.MainActivity
import com.dicoding.skripsiapp.databinding.FragmentLoginBinding
import com.dicoding.skripsiapp.dialog.setupBottomSheetDialog
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentLogin) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarInsets.top, 0, 0) // Tambahkan inset atas
            insets
        }

        binding.tvDontHaveAccount.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
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
                R.id.btn_login, ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM
            )
            constraintSet.setMargin(R.id.btn_login, ConstraintSet.BOTTOM, 16)
        } else {
            constraintSet.connect(
                R.id.textView4, ConstraintSet.START,
                R.id.guidelineLoginLeft, ConstraintSet.START
            )
        }

        constraintSet.applyTo(constraintLayout)


        binding.apply {
            btnLogin.setOnClickListener {
                val email = edtEmail.text.toString().trim()
                val password = edtPassword.text.toString()

                if (email.isEmpty() || password.isEmpty()){
                    Toast.makeText(requireContext(), "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.login(email, password)
            }
        }

        binding.tvForgotPasswordLogin.setOnClickListener {
            setupBottomSheetDialog { email ->
                viewModel.resetPassword(email)
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //colect login state
                launch {
                    viewModel.login.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.btnLogin.startAnimation()
                            }
                            is Resource.Success -> {
                                binding.btnLogin.revertAnimation()
                                Intent(requireActivity(), MainActivity::class.java).also { intent ->
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                            }
                            is Resource.Error -> {
                                binding.btnLogin.revertAnimation()
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                            }
                            else -> Unit
                        }
                    }
                }

                // Collect reset password state
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}