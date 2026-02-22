package com.dicoding.skripsiapp.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.skripsiapp.api.OpenAiRepository
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.fragment.main.bookmarked.BookmarkRepository
import com.dicoding.skripsiapp.util.Classifier
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.Event
import com.dicoding.skripsiapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class PageDetectionViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val auth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
): ViewModel() {

    private val _predictionResult = MutableStateFlow<Resource<List<BoundingBox>>>(Resource.Unspecified())
    val predictionResult: StateFlow<Resource<List<BoundingBox>>> get() = _predictionResult

    private val _inferenceTime = MutableStateFlow<Resource<Long>>(Resource.Unspecified())
    val inferenceTime: StateFlow<Resource<Long>> get() = _inferenceTime

    private val _errorMessage = MutableStateFlow<Resource<String>>(Resource.Unspecified())
    val errorMessage: StateFlow<Resource<String>> get() = _errorMessage

    private val _annotatedBitmap = MutableStateFlow<Resource<Bitmap>>(Resource.Unspecified())
    val annotatedBitmap: StateFlow<Resource<Bitmap>> get() = _annotatedBitmap

    private val _funFact = MutableStateFlow<Resource<String>>(Resource.Unspecified())
    val funFact: StateFlow<Resource<String>> get() = _funFact

    private val _bookmarks = MutableLiveData<List<BookmarkItem>>()
    val bookmarks: LiveData<List<BookmarkItem>> get() = _bookmarks

    private var currentImageUrl: String? = null

    private val _toastMessage = MutableSharedFlow<Event<String>>(extraBufferCapacity = 2)
    val toastMessage: SharedFlow<Event<String>> get() = _toastMessage

    private val _classificationResult = MutableStateFlow<Resource<String>>(Resource.Unspecified())
    val classificationResult: StateFlow<Resource<String>> get() = _classificationResult

    private lateinit var detector: Detector
    private lateinit var classifier: Classifier

    fun initializeDetector(context: Context, modelPath: String, labelsPath: String) {
        detector = Detector(context, modelPath, labelsPath, object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                _predictionResult.value = Resource.Success(emptyList())
                _classificationResult.value = Resource.Error("No objects detected.")
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                _predictionResult.value = Resource.Success(boundingBoxes)
                viewModelScope.launch {
                    sendToastMessage("${boundingBoxes.size} objects detected!")
                }
            }
        })
        detector.setup()
    }

    fun initializeClassifier(context: Context, modelPath: String, labelsPath: String) {
        val labels = Classifier.loadLabels(context, labelsPath)
        classifier = Classifier(context, modelPath, labels)
    }


    private fun detectImage(bitmap: Bitmap) {
        _predictionResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                detector.detect(bitmap)
            } catch (e: Exception) {
                _predictionResult.value = Resource.Error("Error during detection: ${e.message}")
            }
        }
    }

    fun classifyCroppedImages(originalBitmap: Bitmap, boundingBoxes: List<BoundingBox>) {
        _classificationResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val results = mutableListOf<Triple<String, Float, Int>>() // (Label, Confidence, Original Index)

                boundingBoxes.forEachIndexed { index, box ->
                    Log.d("BoundingBox", "Class: ${box.clsName}, x1: ${box.x1}, y1: ${box.y1}, x2: ${box.x2}, y2: ${box.y2}")
                    Log.d("BoundingBoxProcessing", "Processing bounding box $index: ${box.clsName}")

                    val croppedBitmap = cropBitmap(originalBitmap, box)
                    val classificationWithConfidence = classifier.classifyWithConfidence(croppedBitmap)
                    val bestResult = classificationWithConfidence.maxByOrNull { it.second } ?: ("Unknown" to 0f)

                    results.add(Triple(bestResult.first, bestResult.second, index)) // Simpan (Label, Confidence, Original Index)
                    Log.d("ClassificationResult", "Result for bounding box $index: $bestResult")
                }

                // Mapping prioritas
                val priorityMap = mapOf("Aedes" to 1, "Culex" to 1, "Unknown" to 0)

                // Sort berdasarkan prioritas kelas (Aedes & Culex lebih dulu), lalu confidence
                val sortedResults = results.sortedWith(
                    compareByDescending<Triple<String, Float, Int>> { priorityMap[it.first] ?: 0 }
                        .thenByDescending { it.second }
                )

                // Reset indeks per kelas tetapi tetap mempertahankan urutan bounding box asli
                val classCounters = mutableMapOf<String, Int>()
                val indexedResults = sortedResults.map { (label, confidence, originalIndex) ->
                    val newIndex = classCounters.getOrDefault(label, 0) + 1
                    classCounters[label] = newIndex // Update index per kategori

                    Triple(label, confidence, originalIndex to newIndex) // (Label, Confidence, (OriginalIndex, NewIndex))
                }

                // Urutkan kembali berdasarkan bounding box asli (bukan urutan setelah sorting)
                val finalResults = indexedResults.sortedBy { it.third.first } // Sort by OriginalIndex

                val formattedResults = finalResults.map { (label, confidence, indexes) ->
                    val (_, newIndex) = indexes
                    "$label ($newIndex) ${String.format("%.2f", confidence * 100)}%"
                }.joinToString("\n")

                _classificationResult.value = Resource.Success(formattedResults)
            } catch (e: Exception) {
                _classificationResult.value = Resource.Error("Error during classification: ${e.message}")
            }
        }
    }

//    fun classifyCroppedImages(originalBitmap: Bitmap, boundingBoxes: List<BoundingBox>) {
//        _classificationResult.value = Resource.Loading()
//        viewModelScope.launch {
//            try {
//                val results = mutableMapOf<String, Float>() // Menyimpan semua hasil klasifikasi
//
//                boundingBoxes.forEach { box ->
//                    Log.d("BoundingBox", "Class: ${box.clsName}, x1: ${box.x1}, y1: ${box.y1}, x2: ${box.x2}, y2: ${box.y2}")
//                    val croppedBitmap = cropBitmap(originalBitmap, box)
//                    val classificationWithConfidence = classifier.classifyWithConfidence(croppedBitmap)
//
//                    // Ambil hasil klasifikasi dengan confidence tertinggi untuk setiap bounding box
//                    classificationWithConfidence.forEach { (label, confidence) ->
//                        results[label] = (results[label] ?: 0f) + confidence
//                    }
//                }
//
//                // Urutkan hasil berdasarkan confidence tertinggi
//                val sortedResults = results.entries.sortedByDescending { it.value }
//
//                // Format hasil untuk ditampilkan
//                val formattedResults = sortedResults.joinToString("\n") { (label, confidence) ->
//                    "$label: ${String.format("%.2f", confidence * 100)}%"
//                }
//
//                _classificationResult.value = Resource.Success(formattedResults)
//            } catch (e: Exception) {
//                _classificationResult.value = Resource.Error("Error during classification: ${e.message}")
//            }
//        }
//    }

    private fun cropBitmap(bitmap: Bitmap, box: BoundingBox): Bitmap {
        val absoluteBox = convertToAbsoluteCoordinates(box, bitmap.width, bitmap.height)
        Log.d("BoundingBox Conversion", "Original: ${box.x1}, ${box.y1}, ${box.x2}, ${box.y2}")
        Log.d("BoundingBox Conversion", "Converted: ${absoluteBox.x1}, ${absoluteBox.y1}, ${absoluteBox.x2}, ${absoluteBox.y2}")

        return Bitmap.createBitmap(
            bitmap,
            absoluteBox.x1.toInt(),
            absoluteBox.y1.toInt(),
            (absoluteBox.x2 - absoluteBox.x1).toInt(),
            (absoluteBox.y2 - absoluteBox.y1).toInt()
        )
    }

    fun overlayBoundingBoxes(originalBitmap: Bitmap, boundingBoxes: List<BoundingBox>) {
        _annotatedBitmap.value = Resource.Loading()
        try {
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: throw IllegalStateException("Failed to copy bitmap")
            val canvas = Canvas(mutableBitmap)

            val boxPaint = Paint().apply {
                color = Color.RED
                strokeWidth = 5f
                style = Paint.Style.STROKE
            }

            val textPaint = Paint().apply {
                color = Color.BLUE
                textSize = mutableBitmap.width * 0.05f
                isAntiAlias = true
            }

            val labelCounts = mutableMapOf<String, Int>()

            boundingBoxes.forEach { box ->
                val absoluteBox = convertToAbsoluteCoordinates(box, mutableBitmap.width, mutableBitmap.height)
                canvas.drawRect(absoluteBox.x1, absoluteBox.y1, absoluteBox.x2, absoluteBox.y2, boxPaint)

                val count = labelCounts.getOrDefault(box.clsName, 0) + 1
                labelCounts[box.clsName] = count

                canvas.drawText(
                    "${box.clsName} ($count) (${String.format("%.2f", box.cnf * 100)}%)",
                    absoluteBox.x1,
                    maxOf(absoluteBox.y1 - 10, textPaint.textSize),
                    textPaint
                )
            }

            _annotatedBitmap.value = Resource.Success(mutableBitmap)
        } catch (e: Exception) {
            _annotatedBitmap.value = Resource.Error("Error overlaying bounding boxes: ${e.message}")
        }
    }

    fun processImage(bitmap: Bitmap, cameraImageUri: Uri?, contentResolver: ContentResolver) {
        val rotatedBitmap = rotateImageToPortrait(bitmap, cameraImageUri, contentResolver)
        val resizedBitmap = resizeBitmap(rotatedBitmap, 640, 640)
        detectImage(resizedBitmap)
    }

    private fun rotateImageToPortrait(bitmap: Bitmap, uri: Uri?, contentResolver: ContentResolver): Bitmap {
        return try {
            uri?.let { contentResolver.openInputStream(it)?.use { inputStream ->
                val orientation = ExifInterface(inputStream)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

                val rotateAngle = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                if (rotateAngle != 0f) {
                    val matrix = Matrix().apply { postRotate(rotateAngle) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } } ?: bitmap
        } catch (e: Exception) {
            Log.e("PredictionViewModel", "Error rotating image: ${e.message}", e)
            bitmap
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap

        val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return if (newWidth == bitmap.width && newHeight == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }


    private fun convertToAbsoluteCoordinates(box: BoundingBox, imageWidth: Int, imageHeight: Int): BoundingBox {
        return box.copy(
            x1 = box.x1 * imageWidth, y1 = box.y1 * imageHeight,
            x2 = box.x2 * imageWidth, y2 = box.y2 * imageHeight,
            cx = box.cx * imageWidth, cy = box.cy * imageHeight,
            w = box.w * imageWidth, h = box.h * imageHeight
        )
    }

    fun loadTrainingMetrics(context: Context): JSONObject {
        return try {
            val jsonString = context.assets.open("training_metrics.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                jsonArray.getJSONObject(0)
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            Log.e("PredictionViewModel", "Error loading training metrics", e)
            JSONObject()
        }
    }

    fun loadConfusionMatrix(context: Context): Pair<List<List<Float>>, List<String>> {
        val jsonString = context.assets.open("confusion_matrix_YOLOv8.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val matrix = jsonObject.getJSONArray("confusion_matrix")
        val classes = jsonObject.getJSONArray("classes")

        val confusionMatrix = mutableListOf<List<Float>>()
        for (i in 0 until matrix.length()) {
            val row = matrix.getJSONArray(i)
            val rowList = mutableListOf<Float>()
            for (j in 0 until row.length()) {
                rowList.add(row.getDouble(j).toFloat())
            }
            confusionMatrix.add(rowList)
        }

        val classList = mutableListOf<String>()
        for (i in 0 until classes.length()) {
            classList.add(classes.getString(i))
        }

        return Pair(confusionMatrix, classList)
    }

    fun fetchFunFact(className: String) {
        _funFact.value = Resource.Loading()

        // List kata sifat acak untuk membuat prompt lebih unik
        val randomWords = listOf("amazing", "fascinating", "shocking", "unbelievable", "intriguing")
        val randomWord = randomWords.random() // Pilih satu kata secara acak

        // List prompt dengan variasi pertanyaan
        val prompts = listOf(
            "Give me a $randomWord fact about the mosquito genus $className.",
            "What's an $randomWord detail about the mosquito species $className?",
            "Tell me something $randomWord about $className mosquitoes.",
            "Share a rare and $randomWord fact related to $className mosquitoes.",
            "Can you tell me an unusual fact about mosquitoes from genus $className?"
        )

        val selectedPrompt = prompts.random() // Pilih satu prompt secara acak
        val uniquePrompt = "$selectedPrompt (Request ID: ${System.currentTimeMillis()})" // Tambahkan timestamp

        val repository = OpenAiRepository()
        viewModelScope.launch {
            try {
                val funFact = repository.fetchFunFact(uniquePrompt)
                _funFact.value = Resource.Success(funFact)
            } catch (e: Exception) {
                _funFact.value = Resource.Error("Error fetching fun fact: ${e.message}")
            }
        }
    }

    fun getTopPredictionClass(): String? {
        // Ambil nilai dari Resource
        val resource = predictionResult.value ?: return null

        // Periksa status Resource
        return if (resource is Resource.Success) {
            resource.data?.maxByOrNull { it.cnf }?.clsName
        } else {
            null
        }
    }
    private val imageCache = mutableMapOf<Int, String>()

    suspend fun uploadBitmapToFirebase(bitmap: Bitmap): String? {
        val imageHash = bitmap.hashCode()

        // Gunakan URL yang sudah tersimpan jika ada
        if (imageCache.containsKey(imageHash)) {
            return imageCache[imageHash]
        }

        val imageId = "imageDetection_${imageHash}.jpg"
        val storageRef = firebaseStorage.reference.child("detection/$imageId")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

        return try {
            storageRef.putBytes(data).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            imageCache[imageHash] = downloadUrl // Simpan URL dalam cache
            downloadUrl
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Error uploading bitmap: ${e.message}")
            null
        }
    }

    fun addBookmark(bookmarkItem: BookmarkItem) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                bookmarkRepository.addBookmark(bookmarkItem, userId)
                fetchBookmarks() // Refresh data dari Firestore
            } catch (e: Exception) {
                Log.e("Firestore", "Error adding bookmark: ${e.message}")
            }
        }
    }

    fun removeBookmarkByUri(imageUri: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                bookmarkRepository.removeBookmarkByUri(imageUri, userId)
                val updatedBookmarks = bookmarkRepository.getBookmarks(userId)
                _bookmarks.postValue(updatedBookmarks)
                fetchBookmarks()
            } catch (e: Exception) {
                Log.e("Firestore", "Error removing bookmark by URI: ${e.message}")
            }
        }
    }

    fun fetchBookmarks() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val bookmarks = bookmarkRepository.getBookmarks(userId)
                _bookmarks.postValue(bookmarks)
            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching bookmarks: ${e.message}")
            }
        }
    }


    fun sendToastMessage(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(Event(message))
        }
    }

    fun clearImageData() {
        _predictionResult.value = Resource.Unspecified()
        _inferenceTime.value = Resource.Unspecified()
        _errorMessage.value = Resource.Unspecified()
        _annotatedBitmap.value = Resource.Unspecified()
        _funFact.value = Resource.Unspecified()
        _classificationResult.value = Resource.Unspecified()
        currentImageUrl = null
    }


    override fun onCleared() {
        super.onCleared()
        if (::detector.isInitialized) {
            detector.clear()
        }
        _predictionResult.value = Resource.Unspecified()
        _inferenceTime.value = Resource.Unspecified()
        _errorMessage.value = Resource.Unspecified()
    }
}