package com.dicoding.skripsiapp.data

data class YoloSegResult(
    val detections: Array<FloatArray>,
    val proto: Array<Array<FloatArray>>
)