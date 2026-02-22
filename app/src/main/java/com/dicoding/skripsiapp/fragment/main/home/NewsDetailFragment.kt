package com.dicoding.skripsiapp.fragment.main.home

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.databinding.FragmentNewsDetailBinding
import com.dicoding.skripsiapp.util.hideBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.AllCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<NewsDetailFragmentArgs>()

    private val viewModel by viewModels<AllCategoryViewModel>()

    private var currentNews: News? = null
    private var isFavorite: Boolean = false

    private var previousScrollY = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        hideBottomNavigationView()
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.fitsSystemWindows = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabFavorite) { views, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            views.setPadding(0, systemBarInsets.top, 0, 0) // Tambahkan padding bawah berdasarkan insets
            insets
        }

        // Menyesuaikan AppBarLayout dengan insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0) // Tambahkan inset atas
            insets
        }

        // Menyesuaikan NestedScrollView dengan insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedScrollDetail) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0) // Tambahkan inset bawah
            insets
        }

        // Get news object from arguments
        currentNews = arguments?.getParcelable("news")
        if (currentNews == null) {
            Toast.makeText(requireContext(), "News data is missing", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp() // Kembali ke fragment sebelumnya
            return
        }

        currentNews?.let { news ->
            observeFavoriteStatus(news.id!!)
        }

        binding.fabFavorite.setOnClickListener {
            currentNews?.let { news ->
                toggleFavorite(news)
            }
        }

        binding.nestedScrollDetail.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Jika scroll ke bawah (lebih jauh dari sebelumnya), sembunyikan FAB
            if (scrollY > previousScrollY) {
                if (binding.fabFavorite.isShown) {
                    binding.fabFavorite.hide() // Menyembunyikan FAB secara penuh
                }
            }
            // Jika scroll ke atas, tampilkan kembali FAB
            else if (scrollY < previousScrollY) {
                if (!binding.fabFavorite.isShown) {
                    binding.fabFavorite.show() // Menampilkan kembali FAB
                }
            }
            // Update posisi scroll sebelumnya
            previousScrollY = scrollY
        }



        // Pastikan toolbar terlihat di awal
        binding.appBarLayout.translationY = 0f
        val news = args.news

        binding.apply {
            ivBack.setOnClickListener {
                findNavController().navigateUp()
            }
            tvNewsSource.text = news.newsSource
            tvCategory.text = news.category
            tvTitle.text = news.title
            tvAuthor.text = news.author
            ivLink.setOnClickListener{
                val link = news.link
                if (!link.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(link)
                    startActivity(intent)
                } else {
                    // Opsional: Tampilkan pesan jika link kosong
                    Toast.makeText(requireContext(), "Link tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }

            // Cek jika sourceLogoUrls tidak kosong
            if (!news.sourceLogoUrls.isNullOrEmpty()) {
                Glide.with(this@NewsDetailFragment)
                    .load(news.sourceLogoUrls[0])
                    .error(R.drawable.ic_error)
                    .into(ivLogo)
            } else {
                binding.ivLogo.setImageResource(R.drawable.ic_error) // Set default jika kosong
            }

// Cek jika contentImageUrls tidak kosong
            if (!news.contentImageUrls.isNullOrEmpty()) {
                Glide.with(this@NewsDetailFragment)
                    .load(news.contentImageUrls[0])
                    .error(R.drawable.ic_error)
                    .into(ivContent)
            } else {
                binding.ivContent.setImageResource(R.drawable.ic_error) // Set default jika kosong
            }


            tvDescription.text = HtmlCompat.fromHtml(
                news.description.toString(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }

    private fun observeFavoriteStatus(newsId: String) {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteNews.collectLatest { favoriteList ->
                        isFavorite = favoriteList.contains(newsId)
                        updateFavoriteButtonState()
                    }
                }
            }
        }
    }

    private fun toggleFavorite(news: News) {
        if (isFavorite) {
            viewModel.removeFavorite(news.id!!)
            Toast.makeText(requireContext(), getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show()
        } else {
            viewModel.addFavorite(news)
            Toast.makeText(requireContext(), getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show()
        }

        parentFragmentManager.setFragmentResult("favorite_updated", Bundle().apply {
            putBoolean("isUpdated", true)
        })

        viewModel.fetchFavoriteNews()
    }

    private fun updateFavoriteButtonState() {
        binding.fabFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}