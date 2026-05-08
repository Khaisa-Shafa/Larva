package com.dicoding.skripsiapp.fragment.main.favorite

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.adapter.AllNewsAdapter
import com.dicoding.skripsiapp.databinding.FragmentFavoriteBinding
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.util.showBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val allNewsAdapter = AllNewsAdapter()
    private val viewModel by viewModels<FavoriteViewModel>()

    // Variabel untuk debounce
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0) // Tambahkan inset atas
            insets
        }

        // Set up RecyclerView
        binding.rvEventFavorite.apply {
            adapter = allNewsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false) // Tambahkan layout manager
            setHasFixedSize(true)
        }

        // Observe favorite news
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteNews.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> showLoading()
                        is Resource.Success -> {
                            hideLoading()
                            val data = resource.data ?: emptyList()
                            allNewsAdapter.differ.submitList(resource.data)

                            if (data.isEmpty()) {
                                binding.textNoData.visibility = View.VISIBLE
                                binding.rvEventFavorite.visibility = View.GONE
                            } else {
                                binding.textNoData.visibility = View.GONE
                                binding.rvEventFavorite.visibility = View.VISIBLE
                            }

                            // Update data favorit di adapter
                            val favoriteIds = resource.data?.mapNotNull { it.id } ?: emptyList()
                            allNewsAdapter.updateFavorites(favoriteIds)

                        }
                        is Resource.Error -> {
                            hideLoading()
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }

        binding.etQuery.addTextChangedListener { text ->
            val query = text?.toString()?.trim() ?: ""
            allNewsAdapter.setSearchQuery(query)
            searchRunnable?.let { handler.removeCallbacks(it) }

            searchRunnable = Runnable {
                if (query.isNotEmpty()) {
                    searchNews(query)
                } else {
                    observeFavoriteNews()
                }
            }

            handler.postDelayed(searchRunnable!!, 500)
        }

        binding.etQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                closeKeyboard() // Tutup keyboard, terlepas dari kondisi input
                if (!binding.etQuery.text.isNullOrEmpty()) {
                    searchNews(binding.etQuery.text.toString())
                } else {
                    Toast.makeText(requireContext(), "Masukkan teks untuk pencarian", Toast.LENGTH_SHORT).show()
                }
                true // Mengembalikan true agar event dianggap selesai
            } else {
                false
            }
        }


        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchFavoriteNews()
            binding.etQuery.text?.clear()
            closeKeyboard()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.btnDelete.apply {
            visibility = View.GONE
            binding.etQuery.addTextChangedListener { text ->
                visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            setOnClickListener {
                binding.etQuery.text?.clear()
                closeKeyboard() // Tambahkan ini untuk menutup keyboard
            }
        }

        allNewsAdapter.onFavoriteClick = { news ->
            if (viewModel.favoriteIds.value.contains(news.id)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Ingin menghapus dari Favorite?")
                    .setPositiveButton("Ya") { _, _ ->
                        viewModel.removeFavorite(news.id!!)

                        Toast.makeText(requireContext(), "Remove from Favorite", Toast.LENGTH_SHORT).show()

                        // **Update RecyclerView agar langsung berubah**
                        allNewsAdapter.differ.submitList(emptyList())
                        observeFavoriteNews()
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            } else {
                viewModel.addFavorite(news)
            }
        }

        setFragmentResultListener("favorite_updated") { _, bundle ->
            val isUpdated = bundle.getBoolean("isUpdated", false)
            if (isUpdated) {
                viewModel.fetchFavoriteNews() // Ambil data terbaru
                observeFavoriteNews()
            }
        }

        allNewsAdapter.onClick = { news ->
            val bundle = Bundle().apply { putParcelable("News", news) }
            findNavController().navigate(R.id.action_favoriteFragment_to_newsDetailFragment, bundle)
        }
    }

    private fun observeFavoriteNews() {
        lifecycleScope.launch {
            viewModel.favoriteNews.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val data = resource.data ?: emptyList()
                        allNewsAdapter.differ.submitList(resource.data)

                        if (data.isEmpty()) {
                            binding.textNoData.visibility = View.VISIBLE
                            binding.rvEventFavorite.visibility = View.GONE
                        } else {
                            binding.textNoData.visibility = View.GONE
                            binding.rvEventFavorite.visibility = View.VISIBLE
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun searchNews(query: String) {
        lifecycleScope.launch {
            viewModel.searchFavoriteNews(query).collectLatest { newsList ->
                allNewsAdapter.differ.submitList(newsList)
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun closeKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etQuery.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()

        viewModel.fetchFavoriteNews()
        observeFavoriteNews()

        lifecycleScope.launch {
            viewModel.favoriteNews.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val data = resource.data ?: emptyList()

                    allNewsAdapter.differ.submitList(resource.data)

                    if (data.isEmpty()) {
                        binding.textNoData.visibility = View.VISIBLE
                        binding.rvEventFavorite.visibility = View.GONE
                    } else {
                        binding.textNoData.visibility = View.GONE
                        binding.rvEventFavorite.visibility = View.VISIBLE
                    }

                    allNewsAdapter.notifyDataSetChanged() // Paksa RecyclerView refresh
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}