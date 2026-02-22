package com.dicoding.skripsiapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dicoding.skripsiapp.data.FavoriteNewsEntity
import com.dicoding.skripsiapp.util.Converters

@Database(entities = [FavoriteNewsEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class) // Tambahkan ini
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteNewsDao(): FavoriteNewsDao
}
