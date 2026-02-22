package com.dicoding.skripsiapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dicoding.skripsiapp.util.Converters

@Entity(tableName = "favorite_news")
data class FavoriteNewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val author: String?,
    val description: String?,
    val sourceLogoUrls: List<String>,
    val link: String?,
    val newsSource: String,
    @TypeConverters(Converters::class) val contentImageUrls: List<String> // Tambahkan TypeConverter
)