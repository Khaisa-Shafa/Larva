package com.dicoding.skripsiapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.util.Classifier
import com.dicoding.skripsiapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveDetectionViewModel @Inject constructor() : ViewModel() {

    private val _classifiedBoxes =
        MutableStateFlow<Resource<List<ClassifiedBox>>>(Resource.Unspecified())
    val classifiedBoxes = _classifiedBoxes.asStateFlow()

    private val _inferenceTime =
        MutableStateFlow<Resource<Long>>(Resource.Unspecified())
    val inferenceTime = _inferenceTime.asStateFlow()

    private var classifier: Classifier? = null

    data class ClassifiedBox(
        val boundingBox: BoundingBox,
        val label: String,
        val confidence: Float,
        val index: Int
    )

    fun initializeClassifier(context: Context, modelPath: String, labelsPath: String) {
        val labels = Classifier.loadLabels(context, labelsPath)
        classifier = Classifier(context, modelPath, labels)
    }

    fun updateDetectionResult(
        boxes: List<BoundingBox>,
        inferenceTime: Long,
        bitmap: Bitmap
    ) {
        _inferenceTime.value = Resource.Success(inferenceTime)

        if (boxes.isEmpty()) {
            _classifiedBoxes.value = Resource.Success(emptyList())
            return
        }

        classify(bitmap, boxes)
    }

    private fun classify(bitmap: Bitmap, boxes: List<BoundingBox>) {
        val cls = classifier ?: return

        viewModelScope.launch(Dispatchers.Default) { // 🔥 penting

            val results = mutableListOf<ClassifiedBox>()
            val counter = mutableMapOf<String, Int>()

            for (box in boxes){
                val crop = cropBitmap(bitmap, box) ?: continue
                val result = cls.classifyWithConfidence(crop)
                val best = result.maxByOrNull { it.second }

                val label = best?.first ?: "Unknown"
                val conf = best?.second ?: 0f

                val index = counter.getOrDefault(label, 0) + 1
                counter[label] = index

                results.add(ClassifiedBox(box, label, conf, index))
            }

            _classifiedBoxes.value = Resource.Success(results)
        }
    }

    private fun cropBitmap(bitmap: Bitmap, box: BoundingBox): Bitmap? {
        if (bitmap.isRecycled) return null

        val x = (box.x1 * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (box.y1 * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((box.x2 - box.x1) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val h = ((box.y2 - box.y1) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

        // ✅ TAMBAH INI — tolak crop yang terlalu kecil
        if (w < 10 || h < 10) {
            Log.w("Classifier", "Crop terlalu kecil: ${w}x${h}, skip")
            return null
        }

        Log.d("Classifier", "Crop: x=$x y=$y w=$w h=$h dari ${bitmap.width}x${bitmap.height}")

        return try {
            Bitmap.createBitmap(bitmap, x, y, w, h)
        } catch (e: Exception) {
            Log.e("Classifier", "createBitmap gagal: ${e.message}")
            null
        }
    }
}