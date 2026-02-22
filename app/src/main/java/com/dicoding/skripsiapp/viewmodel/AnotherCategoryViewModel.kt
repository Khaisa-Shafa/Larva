package com.dicoding.skripsiapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnotherCategoryViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
): ViewModel() {
    private val _anotherNews = MutableStateFlow<Resource<List<News>>>(Resource.Unspecified())
    val anotherNews: StateFlow<Resource<List<News>>> = _anotherNews

    private val anotherPagingInfo = AnotherPagingInfo()

    // Daftar berita favorit
    private val _favoriteNews = MutableStateFlow<List<String>>(emptyList())
    val favoriteNews: StateFlow<List<String>> = _favoriteNews

    init {
        fetchAnotherNews()
        fetchFavoriteNews()

    }

    private fun fetchFavoriteNews() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("Favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Failed to listen for favorite updates: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val favoriteIds = snapshot.documents.mapNotNull { it.id }
                    viewModelScope.launch {
                        _favoriteNews.emit(favoriteIds)
                    }
                }
            }
    }


    fun addFavorite(news: News) {
        val userId = auth.currentUser?.uid ?: return
        val favoriteData = mapOf(
            "id" to news.id,
            "title" to news.title,
            "category" to news.category,
            "author" to news.author,
            "newsSource" to news.newsSource,
            "link" to news.link,
            "description" to news.description,
            "contentImageUrls" to news.contentImageUrls,
            "sourceLogoUrls" to news.sourceLogoUrls
        )

        firestore.collection("users").document(userId)
            .collection("Favorites").document(news.id!!)
            .set(favoriteData)
            .addOnSuccessListener {
                fetchFavoriteNews()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to add favorite: ${e.message}")
            }
    }

    fun removeFavorite(newsId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("Favorites").document(newsId).delete()
            .addOnSuccessListener {
                fetchFavoriteNews()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to remove favorite: ${e.message}")
            }
    }

    fun fetchAnotherNews() {
        if (!anotherPagingInfo.isPagingEnd) {
            viewModelScope.launch {
                _anotherNews.emit(Resource.Loading())

                firestore
                    .collection("News")
                    .whereEqualTo("category", "Another")
                    .orderBy("id")
                    .limit(anotherPagingInfo.anotherNewsPage * 10)
                    .get()
                    .addOnSuccessListener { result ->
                        val allNewsList = result.toObjects(News::class.java)
                        anotherPagingInfo.isPagingEnd = allNewsList == anotherPagingInfo.oldAnotherNews
                        anotherPagingInfo.oldAnotherNews = allNewsList
                        viewModelScope.launch {
                            _anotherNews.emit(Resource.Success(allNewsList))
                        }
                        anotherPagingInfo.anotherNewsPage++
                    }
                    .addOnFailureListener {
                        viewModelScope.launch {
                            _anotherNews.emit(Resource.Error(it.message.toString()))
                        }
                    }
            }
        }
    }

    fun resetPaging() {
        anotherPagingInfo.anotherNewsPage = 1        // Reset halaman ke 1
        anotherPagingInfo.isPagingEnd = false      // Menandakan bahwa paging belum selesai
        anotherPagingInfo.oldAnotherNews = emptyList() // Reset daftar berita lama
    }
}

internal data class AnotherPagingInfo(
    var anotherNewsPage: Long = 1,
    var oldAnotherNews: List<News> = emptyList(),
    var isPagingEnd: Boolean = false
)