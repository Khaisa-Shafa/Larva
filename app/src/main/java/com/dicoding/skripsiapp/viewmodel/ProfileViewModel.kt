package com.dicoding.skripsiapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.User
import com.dicoding.skripsiapp.database.FavoriteNewsDao
import com.dicoding.skripsiapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val favoriteNewsDao: FavoriteNewsDao

) : ViewModel(){

    private val _user = MutableStateFlow<Resource<User>>(Resource.Unspecified())
    val user = _user.asStateFlow()

    private var userListenerRegistration: ListenerRegistration? = null

    init {
        getUser()
    }

    private fun getUser() {
        val userId = auth.uid
        if (userId == null) {
            viewModelScope.launch {
                _user.emit(Resource.Error("User is not logged in."))
            }
            return
        }

        viewModelScope.launch {
            _user.emit(Resource.Loading())
        }

        userListenerRegistration = firestore.collection("user").document(userId)
            .addSnapshotListener { value, error ->
                viewModelScope.launch {
                    if (error != null) {
                        Log.e("ProfileViewModel", "Error fetching user data: ${error.message}")
                        _user.emit(Resource.Error(error.message.toString()))
                    } else {
                        val user = value?.toObject(User::class.java)
                        if (user != null) {
                            Log.d("ProfileViewModel", "User data fetched: $user")
                            _user.emit(Resource.Success(user))
                        } else {
                            Log.w("ProfileViewModel", "User data is null or does not exist.")
                            _user.emit(Resource.Error("User data is null or does not exist."))
                        }
                    }
                }
            }
    }

    fun logout() {
        userListenerRegistration?.remove() // Hentikan listener
        auth.signOut()

        viewModelScope.launch {
            _user.emit(Resource.Unspecified()) // Reset state setelah logout
            favoriteNewsDao.clearFavorites()
        }
    }


    override fun onCleared() {
        super.onCleared()
        userListenerRegistration?.remove()
    }

}