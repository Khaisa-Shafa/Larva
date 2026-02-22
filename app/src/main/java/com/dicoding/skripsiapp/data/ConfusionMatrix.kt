package com.dicoding.skripsiapp.data

import com.google.gson.annotations.SerializedName

data class ConfusionMatrix(
    @SerializedName("classes")
    val classes: List<String>? = null,

    @SerializedName("confusion_matrix")
    val confusionMatrix: List<List<Int>>? = null // Adjusted to match the nested array structure
)