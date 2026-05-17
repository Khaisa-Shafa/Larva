package com.dicoding.skripsiapp.activity

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.databinding.ActivityLiveDetectionBinding
import com.dicoding.skripsiapp.ml.YoloSegHelper
import com.dicoding.skripsiapp.ml.getBestDetection
import com.dicoding.skripsiapp.ml.generateMask
import com.dicoding.skripsiapp.ml.drawMaskAndBox
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class LiveDetectionActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityLiveDetectionBinding
    private lateinit var detector: Detector
    private val viewModel: LiveDetectionViewModel by viewModels()

    // ✅ CameraX — gantikan semua variable USB di sini
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null

    // ✅ Segmentation — sama persis seperti sebelumnya
    private lateinit var yoloSegHelper: YoloSegHelper

    @Volatile private var isProcessing = false
    @Volatile private var latestBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Sama seperti sebelumnya
        val selectedModel = intent.getIntExtra("selected_model", 0)
        detector = Detector(baseContext, MODEL_PATH_DETECTION_NEW, LABELS_PATH_DETECTION_NEW, this)
        detector.setup()
        viewModel.initializeClassifier(baseContext, MODEL_PATH_CLASSIFICATION_NEW, LABELS_PATH_CLASSIFICATION_NEW)

        // ✅ Segmentation helper — tidak berubah
        yoloSegHelper = YoloSegHelper(baseContext)

        cameraExecutor = Executors.newSingleThreadExecutor()

        observeUI()
        checkCameraPermission()
    }

    // ✅ Permission check — sama, tapi tidak ada lagi USB check
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                100
            )
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            binding.cameraStatus.text = "Izin kamera ditolak"
        }
    }

    // ✅ INTI PERUBAHAN: startCamera() menggantikan semua setup USB
    // ✅ startCamera() langsung pakai awaitInstance — tidak ada ListenableFuture sama sekali
    private fun startCamera() {
//        val cameraProviderFuture = getInstance(this)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processFrame(imageProxy) }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                runOnUiThread { binding.cameraStatus.text = "✅ Kamera aktif" }
            } catch (e: Exception) {
                Log.e("CameraX", "Gagal bind: ${e.message}")
                runOnUiThread { binding.cameraStatus.text = "❌ Gagal membuka kamera" }
            }

        }, ContextCompat.getMainExecutor(this)) // ← Executor, bukan ListenableFuture
    }

    private fun processFrame(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        latestBitmap = bitmap

        detector.detect(bitmap)
    }

    override fun onDetect(
        boxes: List<com.dicoding.skripsiapp.data.BoundingBox>,
        inferenceTime: Long
    ) {
        try {
            val bmp = latestBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                ?: run { isProcessing = false; return }

            val segResult = yoloSegHelper.detect(bmp)
            val best = getBestDetection(segResult.detections)

            if (best != null) {
                val mask = generateMask(segResult.proto, best.coeff)
                val resultBitmap = drawMaskAndBox(bmp, mask, best.box, best.classId, best.score)
                runOnUiThread {
                    binding.overlay.setMaskBitmap(resultBitmap)
                }
            }

            viewModel.updateDetectionResult(boxes, inferenceTime, bmp)

        } catch (e: Exception) {
            Log.e("LARVIFY", "onDetect error: ${e.message}", e)
        } finally {
            isProcessing = false  // ← selalu reset meskipun error
        }
    }

    // ✅ onEmptyDetect — tidak berubah
    override fun onEmptyDetect() {
        runOnUiThread {
            // ✅ HAPUS baris ini
            // binding.overlay.setClassifiedResults(emptyList())
            binding.overlay.setMaskBitmap(null) // ← clear mask saja
        }
        isProcessing = false
    }

    // ✅ observeUI — tidak berubah
    private fun observeUI() {
        lifecycleScope.launch {
            viewModel.classifiedBoxes.collect {
                // ✅ HAPUS atau comment baris ini — tidak perlu lagi
                // if (it is Resource.Success)
                //     binding.overlay.setClassifiedResults(it.data ?: emptyList())
            }
        }
        lifecycleScope.launch {
            viewModel.inferenceTime.collect {
                if (it is Resource.Success)
                    binding.inferenceTime.text = "${it.data} ms"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        imageAnalyzer?.clearAnalyzer()
        latestBitmap?.recycle()
        latestBitmap = null
        detector.clear()
    }
}
