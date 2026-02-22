package com.dicoding.skripsiapp.fragment.main.bookmarked.viewpager

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dicoding.skripsiapp.adapter.ClassificationBookmarkAdapter
import com.dicoding.skripsiapp.databinding.FragmentDetectionViewPagerBinding
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkedFragmentDirections
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.DetectionViewPagerViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetectionViewPagerFragment : Fragment() {

    private var _binding: FragmentDetectionViewPagerBinding? = null
    private val binding get() = _binding!!

    private val detectionBookmarkViewModel: DetectionViewPagerViewModel by viewModels()
    private lateinit var detectinoBookmarkAdapter: ClassificationBookmarkAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDetectionViewPagerBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Adapter dan RecyclerView
        detectinoBookmarkAdapter = ClassificationBookmarkAdapter(emptyList()) { bookmarkItem ->

            val action = BookmarkedFragmentDirections
                .actionBookmarkedFragmentToDetailBookmarkFragment(bookmarkItem.id) // Kirim ID bookmark
            findNavController().navigate(action)
        }

        binding.recyclerViewBookmarks.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = detectinoBookmarkAdapter
            setHasFixedSize(false)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            detectionBookmarkViewModel.fetchBookmarks(userId) // Panggil dengan userId yang valid
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    detectionBookmarkViewModel.bookmarks.collect { bookmarks ->
                        Log.d("DetectionViewPagerFragment", "Received ${bookmarks.size} bookmarks")
                        detectinoBookmarkAdapter.updateData(bookmarks)
                    }
                }

                launch {
                    detectionBookmarkViewModel.isLoading.collect {isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
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