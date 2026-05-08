package com.dicoding.skripsiapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectionViewPagerViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> get() = _bookmarks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading


    fun fetchBookmarks(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true // 🔥 Tampilkan loading sebelum mengambil data
            try {
                val data = bookmarkRepository.getBookmarks(userId)
                Log.d("BookmarkViewModel", "Fetched bookmarks: ${data.size}")
                val filteredBookmarks = data.filter { it.type == "Detection" }

                _bookmarks.value = filteredBookmarks
            } catch (e: Exception) {
                Log.e("BookmarkViewModel", "Error fetching bookmarks: ${e.message}")
            } finally {
                _isLoading.value = false // 🔥 Sembunyikan loading setelah selesai
            }
        }
    }

    fun saveBookmark(bookmark: BookmarkItem, userId: String) {
        viewModelScope.launch {
            try {
                bookmarkRepository.addBookmark(bookmark, userId)
                Log.d("BookmarkViewModel", "Bookmark saved: ${bookmark.id}")
            } catch (e: Exception) {
                Log.e("BookmarkViewModel", "Error saving bookmark: ${e.message}")
            }
        }
    }

}