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
class ClassificationViewPagerViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> get() = _bookmarks

    fun fetchBookmarks(userId: String) {
        viewModelScope.launch {
            val data = bookmarkRepository.getBookmarks(userId)
            Log.d("BookmarkViewModel", "Fetched bookmarks: ${data.size}")
            val filteredBookmarks = data.filter { it.type == "Classification" }

            // Set hasil filter ke StateFlow
            _bookmarks.value = filteredBookmarks
        }
    }

}