package com.dicoding.skripsiapp.fragment.loginRegister

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.activity.LoginRegisterActivity
import com.dicoding.skripsiapp.activity.MainActivity
import com.dicoding.skripsiapp.adapter.OnboardingItemsAdapter
import com.dicoding.skripsiapp.data.OnBoardingItem
import com.dicoding.skripsiapp.databinding.FragmentOnBoardingBinding
import com.dicoding.skripsiapp.viewmodel.OnBoardingViewModel
import com.dicoding.skripsiapp.viewmodel.OnBoardingViewModel.Companion.ACCOUNT_OPTIONS_FRAGMENT
import com.dicoding.skripsiapp.viewmodel.OnBoardingViewModel.Companion.MAIN_ACTIVITY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnBoardingFragment : Fragment() {

    private var _binding: FragmentOnBoardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingItemsAdapter: OnboardingItemsAdapter
    private lateinit var indicatorContainer: LinearLayout

    private val viewModel by viewModels<OnBoardingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnBoardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentOnboarding) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarInsets.top, 0, systemBarInsets.bottom) // Tambahkan inset atas
            insets
        }

        setOnboardingItems()
        setupIndicators()
        setCurrentIndicator(0)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            binding.onboardingViewPager.layoutParams.height = WRAP_CONTENT
        }


        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.navigate.collect{
                        when (it) {
                            MAIN_ACTIVITY -> {
                                Intent(requireActivity(), MainActivity::class.java).also { intent ->
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                            }
                            ACCOUNT_OPTIONS_FRAGMENT -> {
                                findNavController().navigate(it)
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun setOnboardingItems() {
        onboardingItemsAdapter = OnboardingItemsAdapter(
            listOf(
                OnBoardingItem(
                    onboardingImage = R.drawable.onboard_1,
                    title = getString(R.string.onboarding_title1),
                    description =  getString(R.string.onboarding_description1)
                ),
                OnBoardingItem(
                    onboardingImage = R.drawable.onboard_2,
                    title = getString(R.string.onboarding_title2),
                    description =  getString(R.string.onboarding_description2)
                ),
                OnBoardingItem(
                    onboardingImage = R.drawable.onboard_3,
                    title = getString(R.string.onboarding_title3),
                    description =  getString(R.string.onboarding_description3)
                )
            )
        )

        binding.onboardingViewPager.adapter = onboardingItemsAdapter
        binding.onboardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })

        (binding.onboardingViewPager.getChildAt(0) as RecyclerView).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER

        binding.imageNext.setOnClickListener {
            if (binding.onboardingViewPager.currentItem+1 < onboardingItemsAdapter.itemCount) {
                binding.onboardingViewPager.currentItem += 1
            } else {
                navigateToMainActivity()
            }
        }

        binding.textSkip.setOnClickListener {
            navigateToMainActivity()
        }
        binding.btnGetStarted.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun setupIndicators() {
        indicatorContainer = binding.indicatorContainer
        val indicators = arrayOfNulls<ImageView>(onboardingItemsAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8, 8, 8, 8)
        for (i in indicators.indices) {
            indicators[i] = ImageView(requireContext())
            indicators[i]?.let {
                it.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.indicator_inactive_background
                    )
                )
                it.layoutParams = layoutParams
                indicatorContainer.addView(it)
            }
        }
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = indicatorContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorContainer.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.indicator_active_background
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.indicator_inactive_background
                    )
                )
            }
        }
    }

    private fun navigateToMainActivity() {
        viewModel.startButtonClick()
        findNavController().navigate(R.id.action_onBoardingFragment_to_accountOptionsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}