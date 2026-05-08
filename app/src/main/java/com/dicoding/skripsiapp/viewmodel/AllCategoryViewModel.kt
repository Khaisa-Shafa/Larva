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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AllCategoryViewModel  @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
): ViewModel(){

    private val _sliderNews = MutableStateFlow<Resource<List<News>>>(Resource.Unspecified())
    val sliderNews = _sliderNews.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Resource.Unspecified()
    )


    private val _allNews = MutableStateFlow<Resource<List<News>>>(Resource.Unspecified())
    val allNews: StateFlow<Resource<List<News>>> = _allNews

    // Daftar berita favorit
    private val _favoriteNews = MutableStateFlow<List<String>>(emptyList())
    val favoriteNews: StateFlow<List<String>> = _favoriteNews

    private val paginInfo = PagingInfo()

    init {
        Log.d("FirestoreDebug", "ViewModel initialized")
        fetchSliderNews()
        fetchAllNews()
        fetchFavoriteNews()
    }

    fun fetchFavoriteNews() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("user").document(userId)
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
        viewModelScope.launch {
            try {
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
                firestore.collection("user").document(userId)
                    .collection("Favorites").document(news.id!!)
                    .set(favoriteData)
                    .await()
                fetchFavoriteNews()
            } catch (e: Exception) {
                Log.e("FirestoreError", "Failed to add favorite: ${e.message}")
            }
        }
    }

    fun removeFavorite(newsId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("user").document(userId)
            .collection("Favorites").document(newsId).delete()
            .addOnSuccessListener {
                fetchFavoriteNews()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to remove favorite: ${e.message}")
            }
    }

    fun fetchSliderNews() {
        viewModelScope.launch {
            try {
                _sliderNews.emit(Resource.Loading())

                val categories = listOf("Aedes", "Culex", "Anopheles")
                val result = firestore.collection("News")
                    .whereIn("category", categories)
                    .get()
                    .await()

                val sliderNewsList = result.toObjects(News::class.java)
                Log.d("FirestoreDebug", "Slider news count: ${sliderNewsList.size}")
                _sliderNews.emit(Resource.Success(sliderNewsList))

                result.documents.forEach { doc ->
                    Log.d("FirestoreDebug", "Slider doc: ${doc.id} → ${doc.data}")
                }

            } catch (e: Exception) {
                _sliderNews.emit(Resource.Error(e.message.toString()))
                Log.e("FirestoreError", "Failed to fetch slider news: ${e.message}")
            }

        }

    }

    fun fetchAllNews() {
        viewModelScope.launch {
            try {
                _allNews.emit(Resource.Loading())
                Log.d("FirestoreDebug", "Fetching news...")

                val categories = listOf("Aedes", "Culex", "Anopheles")

                val result = firestore.collection("News")  // coba ganti "News" → "news" kalau tidak muncul
                    .whereIn("category", categories)
                    .get()
                    .await()

                Log.d("FirestoreDebug", "Total docs: ${result.documents.size}")
                result.documents.forEach { doc ->
                    Log.d("FirestoreDebug", "Doc: ${doc.id} → ${doc.data}")
                }

                val allNewsList = result.toObjects(News::class.java)
                Log.d("FirestoreDebug", "All news count: ${allNewsList.size}")
                _allNews.emit(Resource.Success(allNewsList))

            } catch (e: Exception) {
                _allNews.emit(Resource.Error(e.message.toString()))
                Log.e("FirestoreError", "Failed: ${e.message}")
            }
        }
    }

    // Menambahkan metode untuk mereset paging
    fun resetPaging() {
        paginInfo.allNewsPage = 1        // Reset halaman ke 1
        paginInfo.isPagingEnd = false      // Menandakan bahwa paging belum selesai
        paginInfo.oldAllNews = emptyList() // Reset daftar berita lama
    }

}

internal data class PagingInfo(
    var allNewsPage: Long = 1,
    var oldAllNews: List<News> = emptyList(),
    var isPagingEnd: Boolean = false
)