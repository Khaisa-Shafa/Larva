package com.dicoding.skripsiapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class News(
    val id: String? = null,
    val title: String,
    val category: String, // aedes, culex, another
    val author: String? = null,
    val newsSource: String, // misalnya cnn, kompas
    val link: String? = null,
    val description: String? = null,
    val contentImageUrls: List<String>, // Gambar dari deskripsi atau isi berita
    val sourceLogoUrls: List<String> // Gambar logo sumber berita
): Parcelable{
    constructor() : this(
        id = null,
        title = "",
        category = "",
        author = null,
        newsSource = "",
        link = null,
        description = null,
        contentImageUrls = emptyList(),
        sourceLogoUrls = emptyList()
    )
}
