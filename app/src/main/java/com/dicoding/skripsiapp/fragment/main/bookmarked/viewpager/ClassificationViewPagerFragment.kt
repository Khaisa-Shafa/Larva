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
import com.dicoding.skripsiapp.databinding.FragmentClassificationViewPagerBinding
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkedFragmentDirections
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.ClassificationViewPagerViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClassificationViewPagerFragment : Fragment() {

    private var _binding: FragmentClassificationViewPagerBinding? = null
    private val binding get() = _binding!!

    private val classificationBookmarkViewModel: ClassificationViewPagerViewModel by viewModels()
    private lateinit var classificationBookmarkAdapter: ClassificationBookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentClassificationViewPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Adapter dan RecyclerView
        classificationBookmarkAdapter = ClassificationBookmarkAdapter(emptyList()) { bookmarkItem ->

            val action = BookmarkedFragmentDirections
                .actionBookmarkedFragmentToDetailBookmarkFragment(bookmarkItem.id) // Kirim ID bookmark
            findNavController().navigate(action)
        }

        binding.recyclerViewBookmarks.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = classificationBookmarkAdapter
            setHasFixedSize(false)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            classificationBookmarkViewModel.fetchBookmarks(userId) // Panggil dengan userId yang valid
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    classificationBookmarkViewModel.bookmarks.collect { bookmarks ->
                        Log.d("ClassificationViewPagerFragment", "Received ${bookmarks.size} bookmarks")
                        classificationBookmarkAdapter.updateData(bookmarks)
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