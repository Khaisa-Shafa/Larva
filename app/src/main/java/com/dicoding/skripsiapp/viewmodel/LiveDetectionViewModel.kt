package com.dicoding.skripsiapp.viewmodel

import androidx.lifecycle.ViewModel
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LiveDetectionViewModel @Inject constructor() : ViewModel(){

    private val _detectionState = MutableStateFlow<Resource<List<BoundingBox>>>(Resource.Unspecified())
    val detectionState: StateFlow<Resource<List<BoundingBox>>> = _detectionState.asStateFlow()

    private val _inferenceTime = MutableStateFlow<Resource<Long>>(Resource.Unspecified())
    val inferenceTime: StateFlow<Resource<Long>> = _inferenceTime.asStateFlow()

    fun updateDetectionResult(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        if (boundingBoxes.isEmpty()) {
            _detectionState.value = Resource.Error("No objects detected")
        } else {
            _detectionState.value = Resource.Success(boundingBoxes)
        }
        _inferenceTime.value = Resource.Success(inferenceTime)
    }

    fun clearDetection() {
        _detectionState.value = Resource.Unspecified()
        _inferenceTime.value = Resource.Unspecified()
    }
}