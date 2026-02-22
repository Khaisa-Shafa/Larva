package com.dicoding.skripsiapp.fragment.loginRegister

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.FragmentSplashBinding
import com.dicoding.skripsiapp.util.Constants.DELAY_SPLASH
import com.dicoding.skripsiapp.viewmodel.SettingsViewModel
import com.dicoding.skripsiapp.viewmodel.factory.ViewModelFactory
import com.dicoding.skripsiapp.viewmodel.preferences.SettingPreferences
import android.util.Log


class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val handler = Handler(Looper.getMainLooper())

    private val startTime = System.currentTimeMillis()

    private val settingViewModel : SettingsViewModel by viewModels {
        val sharedPref = SettingPreferences.getInstance(requireActivity().dataStore)
        ViewModelFactory.getInstance(requireContext(), sharedPref)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("STARTUP", "SplashFragment onViewCreated: ${System.currentTimeMillis() - startTime} ms")

        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedSplash) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarInsets.top, 0, 0) // Tambahkan inset atas
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

        handler.postDelayed({
            Log.d("STARTUP", "Before navigate: ${System.currentTimeMillis() - startTime} ms")

            if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                findNavController().navigate(R.id.action_splashFragment_to_onBoardingFragment)
            }

        }, DELAY_SPLASH)

        val animTop = AnimationUtils.loadAnimation(requireActivity(), R.anim.from_top)
        val animBottom = AnimationUtils.loadAnimation(requireActivity(), R.anim.from_bottom)

        binding.tvSplash.animation = animBottom
        binding.ivLogo.animation = animTop

//        checkTheme()
    }

    private fun checkTheme() {
        settingViewModel.getThemeSettings().observe(viewLifecycleOwner) { isDarkModeActive ->
            if (AppCompatDelegate.getDefaultNightMode() ==
                (if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO)
            ) return@observe

            AppCompatDelegate.setDefaultNightMode(
                if (isDarkModeActive)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}