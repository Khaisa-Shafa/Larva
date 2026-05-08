package com.dicoding.skripsiapp.fragment.main.bookmarked

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dicoding.skripsiapp.adapter.ClassificationBookmarkAdapter
import com.dicoding.skripsiapp.databinding.FragmentBookmarkedBinding
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.DetectionViewPagerViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarkedFragment : Fragment() {

    private var _binding: FragmentBookmarkedBinding? = null
    private val binding get() = _binding!!

    private val detectionBookmarkViewModel: DetectionViewPagerViewModel by viewModels()
    private lateinit var detectinoBookmarkAdapter: ClassificationBookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBookmarkedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0) // Tambahkan inset atas
            insets
        }

        // Setup Adapter dan RecyclerView
        detectinoBookmarkAdapter = ClassificationBookmarkAdapter(emptyList()) { bookmarkItem ->

            val action = BookmarkedFragmentDirections
                .actionBookmarkedFragmentToDetailBookmarkFragment(bookmarkItem.id) // Kirim ID bookmark
            findNavController().navigate(action)
        }

        binding.recyclerViewBookmark.apply {
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
                        Log.d("BookmarkedFragment", "Received ${bookmarks.size} bookmarks")
                        detectinoBookmarkAdapter.updateData(bookmarks)

                        if (bookmarks.isEmpty()) {
                            binding.textNoData.visibility = View.VISIBLE
                            binding.recyclerViewBookmark.visibility = View.GONE
                        } else {
                            binding.textNoData.visibility = View.GONE
                            binding.recyclerViewBookmark.visibility = View.VISIBLE
                        }

                        // Sembunyikan shimmer setelah data diterima
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                    }
                }

                launch {
                    detectionBookmarkViewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.shimmerLayout.startShimmer()
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.recyclerViewBookmark.visibility = View.GONE
                            binding.textNoData.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE

                            if (detectionBookmarkViewModel.bookmarks.value.isEmpty()) {
                                binding.textNoData.visibility = View.VISIBLE
                            }
                        }
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