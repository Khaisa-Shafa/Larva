package com.dicoding.skripsiapp.data

interface Metrics {
    val f1Score: Double
    val precision: Double
    val recall: Double
    val support: Double
}
