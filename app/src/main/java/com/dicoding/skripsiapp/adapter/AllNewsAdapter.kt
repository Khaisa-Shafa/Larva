package com.dicoding.skripsiapp.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.databinding.NewsItemBinding

class AllNewsAdapter : RecyclerView.Adapter<AllNewsAdapter.AllNewsViewHolder>() {

    private val favoriteIds = mutableSetOf<String>()
    private var searchQuery: String? = null

    inner class AllNewsViewHolder(private val binding: NewsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(news: News) {
            binding.apply {
                if (news.contentImageUrls.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(news.contentImageUrls[0]) // Akses elemen pertama hanya jika tidak kosong
                        .into(imageNewsRvItem)
                } else {
                    imageNewsRvItem.setImageResource(R.drawable.ic_error) // Set gambar default
                }

                tvAuthor.text = news.author
                tvCategory.text = news.category
                applyHighlightToText(tvTitle, news.title)

                ivFavorite.setImageResource(
                    if (favoriteIds.contains(news.id)) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite
                )

                ivFavorite.setOnClickListener {
                    onFavoriteClick?.invoke(news)
                }

            }
        }

        private fun applyHighlightToText(textView: TextView, text: String) {
            val spannable = SpannableString(text)
            searchQuery?.let { query ->
                if (query.isNotEmpty()) {
                    val startIndex = text.lowercase().indexOf(query.lowercase())
                    val highlightColor = textView.context.getColor(R.color.custom_color_secondary_light) // Warna dari drawable

                    if (startIndex >= 0) {
                        val endIndex = startIndex + query.length
                        spannable.setSpan(
                            BackgroundColorSpan(highlightColor),
                            startIndex,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            textView.text = spannable
        }
    }

    private val diffCallBack = object : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllNewsViewHolder {
        return AllNewsViewHolder(
            NewsItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: AllNewsViewHolder, position: Int) {
        val news = differ.currentList[position]
        holder.bind(news)

        holder.itemView.setOnClickListener {
            onClick?.invoke(news)
        }
    }

    fun updateFavorites(favorites: List<String>) {
        val oldFavoriteIds = favoriteIds.toSet()
        favoriteIds.clear()
        favoriteIds.addAll(favorites)

        differ.currentList.forEachIndexed { index, news ->
            if (oldFavoriteIds.contains(news.id) != favoriteIds.contains(news.id)) {
                notifyItemChanged(index) // Notifikasi hanya untuk item yang favoritnya berubah
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    var onClick: ((News) -> Unit)? = null
    var onFavoriteClick: ((News) -> Unit)? = null

}