package com.dicoding.skripsiapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.databinding.NewsItemBinding

class CulexCategoryAdapter: RecyclerView.Adapter<CulexCategoryAdapter.CulexCategoryViewHolder>() {

    private val favoriteIds = mutableSetOf<String>()


    inner class CulexCategoryViewHolder(private val binding: NewsItemBinding): RecyclerView.ViewHolder(binding.root){
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
                tvTitle.text = news.title

                ivFavorite.setImageResource(
                    if (favoriteIds.contains(news.id)) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite
                )

                ivFavorite.setOnClickListener {
                    onFavoriteClick?.invoke(news)
                }
            }
        }
    }

    private val diffCallBack = object: DiffUtil.ItemCallback<News>(){
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CulexCategoryViewHolder {
        return CulexCategoryViewHolder(
            NewsItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: CulexCategoryViewHolder, position: Int) {
        val news = differ.currentList[position]
        holder.bind(news)

        holder.itemView.setOnClickListener {
            onClick?.invoke(news)
        }
    }

    fun updateFavorites(favorites: List<String>) {
        favoriteIds.clear()
        favoriteIds.addAll(favorites)
        notifyDataSetChanged()
    }

    var onClick: ((News) -> Unit)? = null
    var onFavoriteClick: ((News) -> Unit)? = null
}