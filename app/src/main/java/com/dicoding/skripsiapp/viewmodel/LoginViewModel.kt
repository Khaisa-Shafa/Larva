package com.dicoding.skripsiapp.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
): ViewModel() {

    private val _login = MutableSharedFlow<Resource<FirebaseUser>>()
    val login = _login.asSharedFlow()

    private val _resetPassword = MutableSharedFlow<Resource<String>>()
    val resetPassword = _resetPassword.asSharedFlow()

    fun login(email: String, password: String) {

        if (!isValidEmail(email)) {
            viewModelScope.launch { _login.emit(Resource.Error("Email tidak valid")) }
            return
        }

        if (password.length < 6) {
            viewModelScope.launch { _login.emit(Resource.Error("Password minimal 6 karakter")) }
            return
        }

        viewModelScope.launch { _login.emit(Resource.Loading())}
        firebaseAuth.signInWithEmailAndPassword(
            email, password
        ).addOnSuccessListener {
            viewModelScope.launch {
                it.user?.let {
                    _login.emit(Resource.Success(it))
                }
            }
        }.addOnFailureListener { exception ->
            viewModelScope.launch {
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> "Akun belum terdaftar"
                    is FirebaseAuthInvalidCredentialsException -> "Email atau password salah"
                    else -> exception.message ?: "Terjadi kesalahan"
                }
                _login.emit(Resource.Error(errorMessage))
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPassword.emit(Resource.Loading())
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Success(email))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Error(it.message.toString()))
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}