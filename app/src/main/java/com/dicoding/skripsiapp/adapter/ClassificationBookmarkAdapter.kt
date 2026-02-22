package com.dicoding.skripsiapp.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.Glide
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkedFragmentDirections

class ClassificationBookmarkAdapter(
    private var bookmarks: List<BookmarkItem>,
    private val onItemClick: (BookmarkItem) -> Unit
) : RecyclerView.Adapter<ClassificationBookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageBookmark: ImageView = view.findViewById(R.id.imageBookmark)
        val textClassification: TextView = view.findViewById(R.id.textClassification)
        val textDate: TextView = view.findViewById(R.id.tv_date)
        val cardItem: CardView = view.findViewById(R.id.cardItem) // Pastikan cardView ada di XML
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar) // Pastikan cardView ada di XML
        val textProvince: TextView = view.findViewById(R.id.tv_province)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val item = bookmarks[position]

        Log.d("BookmarkAdapter", "Binding item: ${item.topClassification}, ImageUri: ${item.imageUri}")
        holder.progressBar.visibility = View.VISIBLE // Tampilkan loading indicator

        holder.imageBookmark.load(item.imageUri) {
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_placeholder_image)
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    holder.progressBar.visibility = View.GONE // Sembunyikan loading setelah gambar di-load
                },
                onError = { _, _ ->
                    holder.progressBar.visibility = View.GONE // Sembunyikan loading jika ada error
                }
            )
        }

        holder.textDate.text = item.date
        holder.textClassification.text = item.topClassification

        if (item.province.isEmpty()) {
            holder.textProvince.visibility = View.GONE
        } else {
            holder.textProvince.text = item.province
            holder.textProvince.visibility = View.VISIBLE
        }

        holder.cardItem.setOnClickListener {
            onItemClick(item)
        }

    }


    override fun getItemCount(): Int = bookmarks.size

    fun updateData(newBookmarks: List<BookmarkItem>) {
        Log.d("BookmarkAdapter", "Updating adapter with ${newBookmarks.size} items")
        bookmarks = newBookmarks
        notifyDataSetChanged()
    }
}

