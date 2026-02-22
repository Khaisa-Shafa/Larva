package com.dicoding.skripsiapp.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.util.Constants.FIRST_LAUNCH
import com.dicoding.skripsiapp.util.Constants.ONBOARDING_KEY
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModel(){

    private val _navigate = MutableStateFlow(0)
    val navigate: StateFlow<Int> = _navigate

    companion object{
        const val MAIN_ACTIVITY = 13
        val ACCOUNT_OPTIONS_FRAGMENT = R.id.action_onBoardingFragment_to_accountOptionsFragment
    }

    init {
        val isFirstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH, true)
        val isOnboardingCompleted = sharedPreferences.getBoolean(ONBOARDING_KEY, false)
        val user = firebaseAuth.currentUser

        if (isFirstLaunch) {
            sharedPreferences.edit().putBoolean(FIRST_LAUNCH, false).apply()
        }

        when {
            user != null -> {
                viewModelScope.launch { _navigate.emit(MAIN_ACTIVITY) }
            }
            isOnboardingCompleted -> {
                viewModelScope.launch { _navigate.emit(ACCOUNT_OPTIONS_FRAGMENT) }
            }
            else -> {
                Log.d("OnBoardingViewModel", "Menampilkan onboarding")
            }
        }
    }


    fun startButtonClick() {
        sharedPreferences.edit().putBoolean(ONBOARDING_KEY, true).apply()
    }
}