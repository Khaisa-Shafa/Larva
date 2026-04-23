package com.dicoding.skripsiapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class News(
    val id: String? = null,
    val title: String = "",
    val category: String = "",
    val author: String? = null,
    val newsSource: String = "",
    val link: String? = null,
    val description: String? = null,
    val contentImageUrls: List<String> = emptyList(),
    val sourceLogoUrls: List<String> = emptyList()
) : Parcelable