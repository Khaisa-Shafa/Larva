package com.dicoding.skripsiapp.activity

import android.os.Bundle
import android.view.SurfaceHolder
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.databinding.ActivityLiveDetectionBinding
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_DETECTION
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_DETECTION
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.PreviewDataCallback
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LiveDetectionActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityLiveDetectionBinding
    private lateinit var cameraClient: CameraClient
    private lateinit var surfaceView: AspectRatioSurfaceView

    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewDataCallback: PreviewDataCallback

    private val viewModel: LiveDetectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_live_detection)) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarInsets.top, 0, 0) // Tambahkan inset atas
            insets
        }

        // Inisialisasi komponen UI
        surfaceView = findViewById(R.id.surfaceVieww)

        // Inisialisasi Detector
        detector = Detector(baseContext, MODEL_PATH_DETECTION, LABELS_PATH_DETECTION, this)
        detector.setup()

        // Inisialisasi CameraClient
        cameraClient = CameraClient.newBuilder(this).apply {
            setEnableGLES(true)
            setRawImage(false)
            setCameraStrategy(CameraUvcStrategy(this@LiveDetectionActivity))
            setCameraRequest(
                CameraRequest.Builder()
                    .setFrontCamera(false)
                    .setPreviewWidth(640)
                    .setPreviewHeight(480)
                    .create()
            )
            openDebug(true)
        }.build()

        // Setup SurfaceView callback
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                cameraClient.openCamera(surfaceView)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                cameraClient.setRenderSize(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraClient.closeCamera()
            }
        })

        // Inisialisasi ExecutorService
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inisialisasi PreviewDataCallback
        previewDataCallback = PreviewDataCallback().apply {
            setDetector(detector)
        }

        // Request permissions
        requestPermissions()

        // Mulai deteksi objek
        startObjectDetection()

        observeDetectionState()
    }

    private fun observeDetectionState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.detectionState.collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                binding.overlay.apply {
                                    setResults(result.data ?: emptyList())
                                    invalidate()
                                }
                            }
                            is Resource.Error -> {
                                binding.overlay.setResults(emptyList())
                                binding.overlay.invalidate()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.inferenceTime.collect { result ->
                        if (result is Resource.Success) {
                            binding.inferenceTime.text = "${result.data}ms"
                        }
                    }
                }

            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.RECORD_AUDIO
        )

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
    }

    override fun onEmptyDetect() {
        binding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        viewModel.updateDetectionResult(boundingBoxes, inferenceTime)
    }

    private fun startObjectDetection() {
        // Tambahkan callback untuk mengambil data preview
        cameraClient.addPreviewDataCallBack(previewDataCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraClient.closeCamera()
        detector.clear()
        // Pastikan tidak ada task berjalan sebelum shutdown
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
            try {
                if (!cameraExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    cameraExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                cameraExecutor.shutdownNow()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cameraClient.closeCamera()
        finish()
    }


    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 12
    }
}