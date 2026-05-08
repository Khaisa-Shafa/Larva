package com.dicoding.skripsiapp.fragment.main.categories

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.adapter.AnophelesCategoryAdapter
import com.dicoding.skripsiapp.databinding.FragmentAnophelesCategoryBinding
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.AnophelesCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val tagAnophelesFragment = "AnophelesCategoryFragment"
@AndroidEntryPoint
class AnophelesCategoryFragment : Fragment() {

    private var _binding: FragmentAnophelesCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<AnophelesCategoryViewModel>()
    private lateinit var anophelesAdapter: AnophelesCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAnophelesCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpAnophelesNewsRv()

        binding.tvHeader.text = "Anopheles News"

        anophelesAdapter.onClick = {
            val b = Bundle().apply { putParcelable("News", it) }
            findNavController().navigate(R.id.action_homeFragment_to_newsDetailFragment, b)
        }

        anophelesAdapter.onFavoriteClick = { news ->
            if (viewModel.favoriteNews.value.contains(news.id)) {
                // Tampilkan dialog konfirmasi untuk menghapus dari favorit
                val context = requireContext()
                val builder = AlertDialog.Builder(context).apply {
                    setMessage("Ingin menghapus dari Favorite?")
                    setPositiveButton("Ya") { _, _ ->
                        viewModel.removeFavorite(news.id!!)
                        Toast.makeText(context, "Removed from Favorite", Toast.LENGTH_SHORT).show()
                    }
                    setNegativeButton("Tidak", null) // Tidak melakukan apa-apa jika "Tidak"
                }
                builder.show()
            } else {
                // Jika tidak ada di favorit, tambahkan ke favorit
                viewModel.addFavorite(news)
                Toast.makeText(requireContext(), "Added to Favorite", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    viewModel.anophelesNews.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                showLoading()
                            }
                            is Resource.Success -> {
                                anophelesAdapter.differ.submitList(it.data)
                                hideLoading()
                            }
                            is Resource.Error -> {
                                hideLoading()
                                Log.e(tagAnophelesFragment, it.message.toString())
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.favoriteNews.collectLatest { favorites ->
                        anophelesAdapter.updateFavorites(favorites)
                    }
                }
            }
        }

        binding.nestedScrollViewAllCategory.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (v.getChildAt(0).bottom <= v.height + scrollY) {
                viewModel.fetchAnophelesNews()
            }
        })
    }

    private fun setUpAnophelesNewsRv() {
        anophelesAdapter = AnophelesCategoryAdapter()
        binding.rvAnophelesNews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = anophelesAdapter
        }
    }

    private fun hideLoading() {
        binding.allCategoryProgressbar.visibility = View.GONE
    }

    private fun showLoading() {
        binding.allCategoryProgressbar.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetPaging()
        viewModel.fetchAnophelesNews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}