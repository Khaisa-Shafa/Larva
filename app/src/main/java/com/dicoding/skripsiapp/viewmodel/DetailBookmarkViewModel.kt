package com.dicoding.skripsiapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailBookmarkViewModel @Inject constructor(
    private val repository: BookmarkRepository
)  : ViewModel() {

    private val _bookmarkItem = MutableStateFlow<BookmarkItem?>(null)
    val bookmarkItem: StateFlow<BookmarkItem?> get() = _bookmarkItem

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> get() = _isBookmarked

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    fun fetchBookmark(bookmarkId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val item = repository.getBookmarkById(bookmarkId)
            _bookmarkItem.value = item
            _isBookmarked.value = item != null  // Update state isBookmarked
            _isLoading.value = false
            Log.d("DetailBookmarkViewModel", "Fetched bookmark: $item")
        }
    }

    fun removeBookmark() {
        viewModelScope.launch {
            val bookmark = _bookmarkItem.value
            if (bookmark != null) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    repository.removeBookmarkByUri(bookmark.imageUri, userId)
                    // Update state setelah bookmark dihapus
                    _isBookmarked.value = false
                    Log.d("DetailBookmarkViewModel", "Bookmark removed")
                }
            }
        }
    }


    fun addBookmark(newBookmark: BookmarkItem) {
        viewModelScope.launch {
            // Ambil userId dari FirebaseAuth atau sumber lain
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            repository.addBookmark(newBookmark, userId)
            // Update state bookmark, misalnya:
            _bookmarkItem.value = newBookmark
            _isBookmarked.value = true
            Log.d("DetailBookmarkViewModel", "Bookmark added: $newBookmark")
        }
    }
}