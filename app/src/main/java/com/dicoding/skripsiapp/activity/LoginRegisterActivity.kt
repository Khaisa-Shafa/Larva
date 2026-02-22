package com.dicoding.skripsiapp.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.ActivityLoginRegisterBinding
import com.dicoding.skripsiapp.fragment.loginRegister.SplashFragment
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

@AndroidEntryPoint
class LoginRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginRegisterBinding
    private val startTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {

        Log.d("STARTUP", "Activity created: ${System.currentTimeMillis() - startTime} ms")

        val splashScreen = installSplashScreen()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Log.d("STARTUP", "After super.onCreate: ${System.currentTimeMillis() - startTime} ms")

        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("STARTUP", "After setContentView: ${System.currentTimeMillis() - startTime} ms")

        binding.root.post {
            Log.d("STARTUP", "First draw completed: ${System.currentTimeMillis() - startTime} ms")
        }
    }
}

