package com.dicoding.skripsiapp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.api.OpenAiRepository
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

    private val _funFact = MutableLiveData<Resource<String>>()
    val funFact: LiveData<Resource<String>> get() = _funFact

    private val _bookmarks = MutableLiveData<List<BookmarkItem>>()
    val bookmarks: LiveData<List<BookmarkItem>> get() = _bookmarks

    fun fetchFunFact(className: String) {
        val prompts = listOf(
            "Provide a fun fact about the mosquito genus $className at ${System.currentTimeMillis()}.",
            "Tell me an interesting fact about mosquitoes of genus $className at ${System.currentTimeMillis()}.",
            "What's a cool fact about the mosquito species $className? At ${System.currentTimeMillis()}",
            "Give me a fun fact about $className mosquitoes at ${System.currentTimeMillis()}."
        )

        val randomPrompt = prompts.random() // Memilih prompt secara acak
        val repository = OpenAiRepository()
        _funFact.value = Resource.Loading() // Set loading state

        viewModelScope.launch {
            try {
                if (_funFact.value is Resource.Loading) {
                    val funFact = repository.fetchFunFact(randomPrompt) // Menggunakan prompt acak dengan waktu
                    _funFact.value = Resource.Success(funFact) // Set success state
                }
            } catch (e: Exception) {
                _funFact.value = Resource.Error("Error fetching fun fact: ${e.message}") // Set error state
            }
        }
    }
}