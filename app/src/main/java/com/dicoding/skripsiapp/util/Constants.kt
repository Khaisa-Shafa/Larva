package com.dicoding.skripsiapp.util

object Constants {
    const val USER_COLLECTION = "user"

    // SPLASH
    const val DELAY_SPLASH = 3000L

    // TAG
    const val TAG_REGISTER = "RegisterFragment"

    // ONBOARDING
    const val ONBOARDING_SP = "introductionSP"
    const val ONBOARDING_KEY = "introductionKey"
    const val FIRST_LAUNCH = "first_launch"

    // Model lama (YOLOv8 + MobileNetV3)
    const val MODEL_PATH_DETECTION = "lama/model_detection.tflite"
    const val LABELS_PATH_DETECTION = "lama/labels.txt"
    const val MODEL_PATH_CLASSIFICATION = "lama/larvae_classification_model_tes_70_15_15.tflite"
    const val LABELS_PATH_CLASSIFICATION = "lama/mobilenet_labels.txt"

    // Model baru (YOLO11-Seg + MobileViT)
    const val MODEL_PATH_DETECTION_NEW = "baru/yolo11_seg.tflite"
    const val LABELS_PATH_DETECTION_NEW = "baru/detection_labels.txt"
    const val MODEL_PATH_CLASSIFICATION_NEW = "baru/mobilevit.tflite"
    const val LABELS_PATH_CLASSIFICATION_NEW = "baru/classification_labels.txt"

    // Enum pilihan model
    const val MODEL_YOLOV8_MOBILENETV3 = 1
    const val MODEL_YOLO11_MOBILEVIT = 2

    const val CAMERA_PERMISSION_CODE = 100
}