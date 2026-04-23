package com.dicoding.skripsiapp.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dicoding.skripsiapp.databinding.ActivityLoginRegisterBinding
import com.dicoding.skripsiapp.fragment.loginRegister.SplashFragment
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class LoginRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Cek apakah user sudah login
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Sudah login → langsung ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

