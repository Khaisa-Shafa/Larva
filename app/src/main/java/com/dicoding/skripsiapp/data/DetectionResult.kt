package com.dicoding.skripsiapp.data

data class DetectionResult (
    val box: FloatArray,   // [x, y, w, h]
    val coeff: FloatArray, // [32]
    val classId: Int,
    val score: Float
)