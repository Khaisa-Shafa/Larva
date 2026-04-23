package com.dicoding.skripsiapp.activity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.databinding.ActivityLiveDetectionBinding
import com.dicoding.skripsiapp.util.Constants
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveDetectionActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityLiveDetectionBinding
    private lateinit var detector: Detector

    private val viewModel: LiveDetectionViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 INIT YOLO
        detector = Detector(
            baseContext,
            MODEL_PATH_DETECTION_NEW,
            LABELS_PATH_DETECTION_NEW,
            this
        )
        detector.setup()

        // 🔥 INIT MobileViT
        viewModel.initializeClassifier(
            baseContext,
            MODEL_PATH_CLASSIFICATION_NEW,
            LABELS_PATH_CLASSIFICATION_NEW
        )

        setupWebView()
        observeUI()

        // 🔥 START LOOP CAPTURE
        startFrameLoop()
    }

    // 🔥 LOAD STREAM WIFI
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()

            // ⚠️ GANTI sesuai IP mikroskop kamu
            loadUrl("http://192.168.29.1")
        }
    }

    // 🔥 LOOP AMBIL FRAME
    private fun startFrameLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isProcessing) {
                    val bitmap = captureWebView()
                    bitmap?.let {
                        isProcessing = true
                        detector.detect(it)
                    }
                }
                handler.postDelayed(this, 300) // 🔥 interval
            }
        })
    }

    // 🔥 CAPTURE FRAME DARI WEBVIEW
    private fun captureWebView(): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                binding.webView.width,
                binding.webView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            binding.webView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    // 🔥 HASIL YOLO
    override fun onDetect(boxes: List<com.dicoding.skripsiapp.data.BoundingBox>, inferenceTime: Long) {
        val bitmap = captureWebView() ?: return

        viewModel.updateDetectionResult(boxes, inferenceTime, bitmap)

        isProcessing = false
    }

    override fun onEmptyDetect() {
        binding.overlay.setClassifiedResults(emptyList())
        isProcessing = false
    }

    // 🔥 OBSERVER UI
    private fun observeUI() {
        lifecycleScope.launch {
            viewModel.classifiedBoxes.collect {
                if (it is Resource.Success) {
                    binding.overlay.setClassifiedResults(it.data ?: emptyList())
                }
            }
        }

        lifecycleScope.launch {
            viewModel.inferenceTime.collect {
                if (it is Resource.Success) {
                    binding.inferenceTime.text = "${it.data} ms"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
    }
}