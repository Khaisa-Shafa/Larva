package com.dicoding.skripsiapp.fragment.main.home

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.adapter.CarouselAdapter
import com.dicoding.skripsiapp.databinding.FragmentHomeBinding
import com.dicoding.skripsiapp.fragment.main.categories.AedesCategoryFragment
import com.dicoding.skripsiapp.fragment.main.categories.AllCategoryFragment
import com.dicoding.skripsiapp.fragment.main.categories.AnotherCategoryFragment
import com.dicoding.skripsiapp.fragment.main.categories.CulexCategoryFragment
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.HomeViewpagerAdapter
import com.dicoding.skripsiapp.viewmodel.AllCategoryViewModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "HomeFragment"

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sliderNewsAdapter: CarouselAdapter

    private val viewModel by viewModels<AllCategoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpSliderNewsRv()

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { views, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            views.setPadding(0, systemBarInsets.top, 0, 0) // Tambahkan padding atas
            insets
        }


        // Handle orientation-specific behavior
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewCompat.setFitsSystemWindows(binding.root, true)
            ViewCompat.setOnApplyWindowInsetsListener(binding.tabLayout) { view, insets ->
                val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, 0, 0, 0) // Tambahkan inset bawah
                insets
            }
        } else {
            ViewCompat.setFitsSystemWindows(binding.root, false)
        }

        // Handle slider news click
        sliderNewsAdapter.onClick = {
            val b = Bundle().apply { putParcelable("news", it) }
            findNavController().navigate(R.id.action_homeFragment_to_newsDetailFragment, b)
        }

        // Observe slider news data
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.sliderNews.collectLatest { resource ->
                        try {
                            when (resource) {
                                is Resource.Loading -> showLoading()
                                is Resource.Success -> {
                                    sliderNewsAdapter.differ.submitList(resource.data)
                                    hideLoading()
                                }
                                is Resource.Error -> {
                                    hideLoading()
                                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                                    Log.e(TAG, "Error fetching slider news: ${resource.message}")
                                }
                                else -> Unit
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Unexpected error in sliderNews collection", e)
                        }
                    }
                }
            }
        }

        // Set up ViewPager2 with fragments
        val categoriesFragment = arrayListOf<Fragment>(
            AllCategoryFragment(),
            AedesCategoryFragment(),
            CulexCategoryFragment(),
            AnotherCategoryFragment(),
        )

        binding.viewpagerHome.apply {
            isUserInputEnabled = true
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                isNestedScrollingEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }

        // Handle conflicts between NestedScrollView and ViewPager2
        binding.viewpagerHome.setOnTouchListener { _, event ->
            if (binding.nestedScrollHome.canScrollVertically(-1) || binding.nestedScrollHome.canScrollVertically(1)) {
                // Jika NestedScrollView sedang scroll, matikan swipe di ViewPager2
                false
            } else {
                // Jika tidak ada scroll, aktifkan swipe di ViewPager2
                binding.viewpagerHome.onTouchEvent(event)
            }
        }

        val viewPager2Adapter = HomeViewpagerAdapter(categoriesFragment, childFragmentManager, lifecycle)
        binding.viewpagerHome.adapter = viewPager2Adapter
        binding.viewpagerHome.offscreenPageLimit = categoriesFragment.size

        TabLayoutMediator(binding.tabLayout, binding.viewpagerHome) { tab, position ->
            when (position) {
                0 -> tab.text = "All"
                1 -> tab.text = "Aedes"
                2 -> tab.text = "Culex"
                3 -> tab.text = "Another"
            }
        }.attach()
    }

    private fun setUpSliderNewsRv() {
        sliderNewsAdapter = CarouselAdapter()
        binding.rvSlider.apply {
            layoutManager = CarouselLayoutManager()
            CarouselSnapHelper().attachToRecyclerView(this)
            adapter = sliderNewsAdapter
        }
    }

    private fun hideLoading() {
        binding.progressBarHome.visibility = View.INVISIBLE
    }

    private fun showLoading() {
        binding.progressBarHome.visibility = View.VISIBLE
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
