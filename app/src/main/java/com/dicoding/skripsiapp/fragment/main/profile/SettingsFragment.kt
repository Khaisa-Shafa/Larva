package com.dicoding.skripsiapp.fragment.main.profile

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.FragmentSettingsBinding
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.SettingsViewModel
import com.dicoding.skripsiapp.viewmodel.factory.ViewModelFactory
import com.dicoding.skripsiapp.viewmodel.preferences.SettingPreferences


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val settingViewModel : SettingsViewModel by viewModels {
        val sharedPref = SettingPreferences.getInstance(requireContext().dataStore)
        ViewModelFactory.getInstance(requireContext(), sharedPref)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentSettingProfile) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0) // Tambahkan inset atas
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

        binding.imageCloseUserAccount.setOnClickListener {
            findNavController().navigateUp()
        }

        settingViewModel.getThemeSettings().observe(viewLifecycleOwner) { isDarkModeActive: Boolean ->
            if (isDarkModeActive) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                binding.switchTheme.isChecked = true
                binding.tvEnableDarkMode.text = resources.getString(R.string.disable_dark_mode)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                binding.switchTheme.isChecked = false
                binding.tvEnableDarkMode.text = resources.getString(R.string.enable_dark_mode)
            }
        }

        binding.switchTheme.setOnCheckedChangeListener{ _: CompoundButton?, isChecked: Boolean ->
            settingViewModel.saveThemeSetting(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}