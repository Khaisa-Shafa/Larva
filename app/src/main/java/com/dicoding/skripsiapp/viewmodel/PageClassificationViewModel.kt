package com.dicoding.skripsiapp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PageClassificationViewModel  @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap

    private val _bookmarks = MutableLiveData<List<BookmarkItem>>()
    val bookmarks: LiveData<List<BookmarkItem>> get() = _bookmarks
}