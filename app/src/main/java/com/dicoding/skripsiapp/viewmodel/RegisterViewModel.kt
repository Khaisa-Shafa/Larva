package com.dicoding.skripsiapp.viewmodel

import androidx.lifecycle.ViewModel
import com.dicoding.skripsiapp.data.User
import com.dicoding.skripsiapp.util.Constants.USER_COLLECTION
import com.dicoding.skripsiapp.util.RegisterFieldState
import com.dicoding.skripsiapp.util.RegisterValidation
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.util.validateEmail
import com.dicoding.skripsiapp.util.validatePassword
import com.dicoding.skripsiapp.util.validateRePassword
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseFirestore
):ViewModel() {

    private val _register = MutableStateFlow<Resource<User>>(Resource.Unspecified())
    val register: Flow<Resource<User>> = _register

    private val _validation = Channel<RegisterFieldState>()
    val validation = _validation.receiveAsFlow()

    fun createAccountWithEmailAndPassword(user: User, password: String, rePassword: String) {
        checkValidation(user, password, rePassword)

        if (checkValidation(user, password, rePassword)) {
            runBlocking {
                _register.emit(Resource.Loading())
            }

            firebaseAuth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener {
                    it.user?.let {
                        saveUserInfo(it.uid, user)
                    }
                }
                .addOnFailureListener { exception ->
                    val errorMessage = if (exception is FirebaseAuthUserCollisionException) {
                        "Email Sudah Terdaftar"
                    } else {
                        exception.message ?: "Terjadi kesalahan"
                    }
                    _register.value = Resource.Error(errorMessage)
                }
        } else {
            val registerFieldsState = RegisterFieldState(
                validateEmail(user.email),
                validatePassword(password),
                validateRePassword(password, rePassword)
            )
            runBlocking {
                _validation.send(registerFieldsState)
            }
        }
    }

    private fun saveUserInfo(userUid: String, user: User) {
        db.collection(USER_COLLECTION)
            .document(userUid)
            .set(user)
            .addOnSuccessListener {
                _register.value = Resource.Success(user)
            }
            .addOnFailureListener {
                _register.value = Resource.Error(it.message.toString())
            }
    }

    private fun checkValidation(user: User, password: String, rePassword: String): Boolean {
        val emailValidation = validateEmail(user.email)
        val passwordValidation = validatePassword(password)
        val rePasswordValidation = validateRePassword(password, rePassword)
        val shouldRegister = emailValidation is RegisterValidation.Success
                && passwordValidation is RegisterValidation.Success
                && rePasswordValidation is RegisterValidation.Success

        return shouldRegister
    }
}