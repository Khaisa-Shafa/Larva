package com.dicoding.skripsiapp.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Parcelize
@Keep
data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String = "",
    val topClassification: String = "",
    val fullClassificationResults: String = "",
    val isBookmarked: Boolean = false,
    val type: String = "",
    val date: String = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val province: String = "",
    val city: String = ""
) : Parcelable

