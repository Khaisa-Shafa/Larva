package com.dicoding.skripsiapp.util

data class ModelConfig(
    val detectionModel: String,
    val detectionLabels: String,
    val classificationModel: String,
    val classificationLabels: String
)

fun getModelConfig(selectedModel: Int): ModelConfig {
    return when (selectedModel) {
        Constants.MODEL_YOLOV8_MOBILENETV3 -> ModelConfig(
            detectionModel = Constants.MODEL_PATH_DETECTION,
            detectionLabels = Constants.LABELS_PATH_DETECTION,
            classificationModel = Constants.MODEL_PATH_CLASSIFICATION,
            classificationLabels = Constants.LABELS_PATH_CLASSIFICATION
        )

        Constants.MODEL_YOLO11_MOBILEVIT -> ModelConfig(
            detectionModel = Constants.MODEL_PATH_DETECTION_NEW,
            detectionLabels = Constants.LABELS_PATH_DETECTION_NEW,
            classificationModel = Constants.MODEL_PATH_CLASSIFICATION_NEW,
            classificationLabels = Constants.LABELS_PATH_CLASSIFICATION_NEW
        )

        else -> throw IllegalArgumentException("Unknown model")
    }
}