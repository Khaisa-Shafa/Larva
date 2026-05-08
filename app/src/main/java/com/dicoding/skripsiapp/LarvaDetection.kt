package com.dicoding.skripsiapp

data class LarvaDetection(
    val latitude: Double,
    val longitude: Double,
    val classId: Int,
    val label: String,
    val confidence: Float,
    val timestamp: Long
)

