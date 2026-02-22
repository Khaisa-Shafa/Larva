package com.dicoding.skripsiapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.databinding.SliderItemBinding

class CarouselAdapter: RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    inner class CarouselViewHolder(private val binding: SliderItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(news: News) {
            binding.apply {
                Glide.with(itemView).load(news.contentImageUrls[0]).into(sliderImage)
                tvJudul.text = news.title
                tvNewsSource.text = news.newsSource

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        return CarouselViewHolder(
            SliderItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val news = differ.currentList[position]
        holder.bind(news)

        holder.itemView.setOnClickListener{
            onClick?.invoke(news)
        }
    }

    var onClick: ((News) -> Unit)? = null
}