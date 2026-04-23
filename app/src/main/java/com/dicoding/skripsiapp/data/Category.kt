package com.dicoding.skripsiapp.data

sealed class Category(val category: String) {

    object All: Category("All")
    object Aedes: Category("Aedes")
    object Culex: Category("Culex")
    object Unknown: Category("Anopheles")
}