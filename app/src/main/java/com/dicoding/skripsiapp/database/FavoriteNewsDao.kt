package com.dicoding.skripsiapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dicoding.skripsiapp.data.FavoriteNewsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteNewsDao {

    @Query("SELECT * FROM favorite_news WHERE title LIKE '%' || :query || '%'")
    fun searchFavoriteNews(query: String): Flow<List<FavoriteNewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(news: FavoriteNewsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteNews(news: List<FavoriteNewsEntity>) // Insert batch

    @Query("DELETE FROM favorite_news WHERE id = :newsId")
    suspend fun removeFavoriteById(newsId: String)

    @Query("SELECT * FROM favorite_news")
    fun getFavoriteNews(): Flow<List<FavoriteNewsEntity>>

    @Query("DELETE FROM favorite_news") // Tambahkan fungsi ini
    suspend fun clearFavorites()
}