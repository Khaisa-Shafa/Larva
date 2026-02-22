package com.dicoding.skripsiapp

import android.app.Application
//import com.dicoding.skripsiapp.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SkripsiApp: Application() {
    override fun onCreate() {   
        super.onCreate()
        FirebaseApp.initializeApp(this)

        if (applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0
        ) {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}