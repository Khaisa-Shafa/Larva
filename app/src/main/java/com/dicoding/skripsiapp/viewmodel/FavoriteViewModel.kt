package com.dicoding.skripsiapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.FavoriteNewsEntity
import com.dicoding.skripsiapp.data.News
import com.dicoding.skripsiapp.database.FavoriteNewsDao
import com.dicoding.skripsiapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val favoriteNewsDao: FavoriteNewsDao
) : ViewModel() {

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> get() = _favoriteIds

    private val _favoriteNews = MutableStateFlow<Resource<List<News>>>(Resource.Unspecified())
    val favoriteNews: StateFlow<Resource<List<News>>> get() = _favoriteNews

    init {
        observeFavoriteNewsFromFirestore()
        fetchFavoriteNews()
        viewModelScope.launch {
            favoriteNewsDao.getFavoriteNews().collect { newsList ->
                _favoriteIds.value = newsList.mapNotNull { it.id }.toSet()
            }
        }
    }

    private fun observeFavoriteNewsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("Favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FavoriteViewModel", "Error observing favorites: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val favoriteNews = snapshot.toObjects(News::class.java)

                    // Perbarui data di Room untuk menjaga sinkronisasi
                    viewModelScope.launch {
                        favoriteNewsDao.insertFavoriteNews(
                            favoriteNews.map { news ->
                                FavoriteNewsEntity(
                                    id = news.id!!,
                                    title = news.title,
                                    category = news.category,
                                    author = news.author,
                                    contentImageUrls = news.contentImageUrls,
                                    description = news.description,
                                    sourceLogoUrls = news.sourceLogoUrls,
                                    link = news.link,
                                    newsSource = news.newsSource
                                )
                            }
                        )
                    }

                    // Perbarui _favoriteNews secara langsung
                    _favoriteNews.value = Resource.Success(favoriteNews)
                } else {
                    // Jika tidak ada data, kosongkan Room dan _favoriteNews
                    viewModelScope.launch {
                        favoriteNewsDao.clearFavorites()
                    }
                    _favoriteNews.value = Resource.Success(emptyList())
                }
            }
    }

    fun fetchFavoriteNews() {
        val userId = auth.currentUser?.uid ?: return
        _favoriteNews.value = Resource.Loading()

        firestore.collection("users").document(userId)
            .collection("Favorites")
            .get(Source.SERVER)
            .addOnSuccessListener { result ->
                val favoriteNews = result.toObjects(News::class.java)

                // Simpan ke Room
                viewModelScope.launch {
                    favoriteNewsDao.insertFavoriteNews(
                        favoriteNews.map { news ->
                            FavoriteNewsEntity(
                                id = news.id!!,
                                title = news.title,
                                category = news.category,
                                author = news.author,
                                contentImageUrls = news.contentImageUrls,
                                description = news.description,
                                sourceLogoUrls = news.sourceLogoUrls,
                                link = news.link,
                                newsSource = news.newsSource
                            )
                        }
                    )
                }

                _favoriteNews.value = Resource.Success(favoriteNews)
            }
            .addOnFailureListener {
                _favoriteNews.value = Resource.Error(it.message.toString())
            }
    }

    fun getFavoriteNewsFromRoom(): Flow<List<News>> {
        return favoriteNewsDao.getFavoriteNews()
            .map { newsEntities -> newsEntities.map { it.toNews() } }
    }


    fun addFavorite(news: News) {
        val userId = auth.currentUser?.uid ?: return
        val favoriteData = mapOf(
            "id" to news.id,
            "title" to news.title,
            "category" to news.category,
            "author" to news.author,
            "contentImageUrls" to news.contentImageUrls
        )
        firestore.collection("users").document(userId)
            .collection("Favorites").document(news.id!!)
            .set(favoriteData)
            .addOnSuccessListener {
                // Simpan ke Room setelah sukses update Firestore
                viewModelScope.launch {
                    favoriteNewsDao.insertFavorite(
                        FavoriteNewsEntity(
                            id = news.id,
                            title = news.title,
                            category = news.category,
                            author = news.author,
                            contentImageUrls = news.contentImageUrls,
                            description = news.description,
                            sourceLogoUrls = news.sourceLogoUrls,
                            link = news.link,
                            newsSource = news.newsSource
                        )
                    )
                }
            }
            .addOnFailureListener { e -> Log.e("FavoriteViewModel", "Failed to add favorite: ${e.message}") }
    }

    /** Hapus favorite dari Firestore dan Room */
    fun removeFavorite(newsId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("Favorites").document(newsId)
            .delete()
            .addOnSuccessListener {
                viewModelScope.launch {
                    // Hapus langsung dari Room setelah Firestore dihapus
                    favoriteNewsDao.removeFavoriteById(newsId)

                    // Paksa StateFlow berubah agar RecyclerView refresh
                    val updatedList = _favoriteNews.value.data?.filter { it.id != newsId } ?: emptyList()
                    _favoriteNews.emit(Resource.Success(updatedList))

                    _favoriteIds.value -= newsId

                }
            }
            .addOnFailureListener { e -> Log.e("FavoriteViewModel", "Failed to remove favorite: ${e.message}") }
    }


    /** Cari favorite dari Room */
    fun searchFavoriteNews(query: String): Flow<List<News>> {
        return favoriteNewsDao.searchFavoriteNews(query)
            .map { newsEntities -> newsEntities.map { it.toNews() } }
    }

}

fun FavoriteNewsEntity.toNews() = News(
    id = this.id,
    title = this.title,
    category = this.category,
    author = this.author,
    newsSource = this.newsSource, // Nilai default karena tidak ada di FavoriteNewsEntity
    link = this.link, // Nilai default karena tidak ada di FavoriteNewsEntity
    description = this.description, // Nilai default karena tidak ada di FavoriteNewsEntity
    contentImageUrls = this.contentImageUrls ?: emptyList(),
    sourceLogoUrls = this.sourceLogoUrls // Nilai default karena tidak ada di FavoriteNewsEntity
)