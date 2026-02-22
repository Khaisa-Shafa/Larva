package com.dicoding.skripsiapp.data

import com.google.gson.annotations.SerializedName

data class ModelResults(

    @field:SerializedName("validation_loss")
    val validationLoss: Double,

    @field:SerializedName("test_loss")
    val testLoss: Double,

    @field:SerializedName("training_accuracy")
    val trainingAccuracy: Double,

    @field:SerializedName("test_accuracy")
    val testAccuracy: Double,

    @field:SerializedName("validation_accuracy")
    val validationAccuracy: Double,

    @field:SerializedName("training_loss")
    val trainingLoss: Double
)