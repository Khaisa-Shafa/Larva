package com.dicoding.skripsiapp.activity

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.jiangdg.usb.USBMonitor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.databinding.ActivityLiveDetectionBinding
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_CLASSIFICATION_NEW
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_DETECTION_NEW
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class LiveDetectionActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityLiveDetectionBinding
    private lateinit var detector: Detector
    private val viewModel: LiveDetectionViewModel by viewModels()

    private var cameraClient: CameraClient? = null
    private var isCameraOpen = false
    private var isSurfaceReady = false

    @Volatile private var latestBitmap: Bitmap? = null
    private var isProcessing = false

    private val handler = Handler(Looper.getMainLooper())

    private var retryCount = 0
    private val MAX_RETRY = 3

    private lateinit var logFile: File

    private val isCameraOpenAtomic = AtomicBoolean(false)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            writeLog("📡 Broadcast: ${intent.action} | VID:${device?.vendorId} PID:${device?.productId}")

            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    writeLog("🔌 USB Attached!")
                    handler.removeCallbacksAndMessages("retry_token")
                    handler.postDelayed({
                        retryCount = 0
                        tryOpenCamera()
                    }, 2000)
                }
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    writeLog("🔑 Permission granted: $granted")
                    if (granted) {
                        handler.postDelayed({ tryOpenCamera() }, 2000)
                    } else {
                        binding.cameraStatus.text = "Izin USB ditolak"
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    writeLog("❌ USB Detached")
                    handler.removeCallbacksAndMessages(null)
                    cameraClient?.closeCamera()
                    isCameraOpenAtomic.set(false)
                    binding.cameraStatus.text = "USB dicabut"
                    retryCount = 0
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLiveDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logFile = File(getExternalFilesDir(null), "camera_log.txt")
        logFile.writeText("=== LOG START ===\n")

        detector = Detector(baseContext, MODEL_PATH_DETECTION_NEW, LABELS_PATH_DETECTION_NEW, this)
        detector.setup()

        viewModel.initializeClassifier(baseContext, MODEL_PATH_CLASSIFICATION_NEW, LABELS_PATH_CLASSIFICATION_NEW)

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        setupCameraClient()
        setupTextureView()
        observeUI()
        startFrameLoop()
        checkCameraPermission()
    }

    private fun setupCameraClient() {
        cameraClient?.closeCamera()
        cameraClient = null

        cameraClient = CameraClient.newBuilder(this)
            .setCameraStrategy(CameraUvcStrategy(this))
            .setCameraRequest(
                CameraRequest.Builder()
                    .setFrontCamera(false)
                    .setPreviewWidth(640)
                    .setPreviewHeight(480)
                    .create()
            )
            .setEnableGLES(false)
            .setRawImage(true)
            .openDebug(true)
            .build()

        val strategy = cameraClient?.getCameraStrategy() as? CameraUvcStrategy
        strategy?.setDeviceConnectStatusListener(object : com.jiangdg.ausbc.callback.IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                writeLog("onAttachDev: ${device?.productName}")
            }
            override fun onDetachDec(device: UsbDevice?) {
                writeLog("onDetachDev")
            }
            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                writeLog("onConnectDev ✅ ctrlBlock=${ctrlBlock != null}")
                handler.post { isCameraOpen = true }
            }
            override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                writeLog("onDisConnectDev")
                handler.post { isCameraOpenAtomic.set(false) }
            }
            override fun onCancelDev(device: UsbDevice?) {
                writeLog("onCancelDev")
            }
        })

        Log.d("PREVIEW_DEBUG", "Menambah callback, list sebelumnya: ${strategy?.mPreviewDataCbList?.size}")
        cameraClient?.addPreviewDataCallBack(object : IPreviewDataCallBack {
            override fun onPreviewData(data: ByteArray?, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat) {
                data ?: return
                Log.d("PREVIEW_DEBUG", "onPreviewData size=${data.size}")

                // Data dari ST301 adalah MJPEG, decode langsung dengan BitmapFactory
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: run {
                    Log.e("PREVIEW_DEBUG", "decode gagal, coba yuyvToBitmap")
                    // Fallback ke YUYV kalau MJPEG decode gagal
                    yuyvToBitmap(data, width, height) ?: return
                }

                val old = latestBitmap
                latestBitmap = bitmap
                old?.recycle()
                handler.post { binding.cameraStatus.text = "✅ ${width}x${height}" }
            }
        })
        Log.d("PREVIEW_DEBUG", "List sesudahnya: ${strategy?.mPreviewDataCbList?.size}")
    }

    private fun setupTextureView() {
        binding.textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, w: Int, h: Int) {
                writeLog("Surface ready! w=$w h=$h")
                writeLog("SurfaceTexture: $surface")
                isSurfaceReady = true
                tryOpenCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                isSurfaceReady = false
                cameraClient?.closeCamera()
                isCameraOpen = false
                return true
            }
            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        } else {
            checkAlreadyAttachedDevices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            checkAlreadyAttachedDevices()
        }
    }

    private fun checkAlreadyAttachedDevices() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        if (usbManager.deviceList.isEmpty()) {
            Log.e("USB_SCAN", "⚠️ Tidak ada USB device sama sekali")
            binding.cameraStatus.text = "Colok mikroskop USB..."
            return
        }

        usbManager.deviceList.forEach { (_, device) ->
            Log.d("USB_SCAN", "🔍 Device: ${device.productName} VID:${device.vendorId} PID:${device.productId}")
            if (usbManager.hasPermission(device)) {
                Log.d("USB_SCAN", "✅ Sudah punya permission, langsung buka kamera")
                tryOpenCamera()
            } else {
                Log.d("USB_SCAN", "🔑 Request permission untuk device ini...")
                requestUsbPermission(device)
            }
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            putExtra(UsbManager.EXTRA_DEVICE, device)
            setPackage(packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("USB_SCAN", "📤 Meminta permission USB...")
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun tryOpenCamera() {
        handler.post {
            // FIX: Gunakan compareAndSet agar atomic — hanya satu thread yang bisa masuk
            if (!isCameraOpenAtomic.compareAndSet(false, true)) {
                writeLog("Kamera sudah dibuka atau sedang dibuka, skip")
                return@post
            }
            if (!isSurfaceReady) {
                isCameraOpenAtomic.set(false) // reset kalau gagal
                writeLog("Surface belum siap, tunggu...")
                binding.cameraStatus.text = "Menunggu surface..."
                return@post
            }
            val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
            if (usbManager.deviceList.isEmpty()) {
                isCameraOpenAtomic.set(false) // reset kalau gagal
                writeLog("Tidak ada USB device, tunggu...")
                binding.cameraStatus.text = "Colokkan mikroskop USB..."
                return@post
            }
            writeLog("🚀 Membuka kamera UVC... (attempt ${retryCount + 1})")
            binding.cameraStatus.text = "Menghubungkan kamera USB..."
            try {
                cameraClient?.openCamera(binding.textureView)
                writeLog("✅ openCamera dipanggil")
            } catch (e: Exception) {
                isCameraOpenAtomic.set(false) // reset kalau exception
                writeLog("❌ Error open: ${e.message}")
                retryOpenCamera()
            }
        }
    }

    private fun retryOpenCamera() {
        if (retryCount >= MAX_RETRY) {
            writeLog("❌ Max retry reached")
            handler.post { binding.cameraStatus.text = "Gagal connect, cabut & colok ulang" }
            return
        }
        retryCount++
        writeLog("🔄 Retry ke-$retryCount dalam 1.5 detik...")
        handler.postDelayed({
            isCameraOpen = false
            tryOpenCamera()
        }, 1500)
    }

    private fun startFrameLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (isCameraOpenAtomic.get()) {
                    val bmp = binding.textureView.getBitmap(640, 480)
                    Log.d("FRAME_LOOP", "getBitmap result=${bmp != null} isAvailable=${binding.textureView.isAvailable}")
                    if (bmp != null && !isProcessing) {
                        val old = latestBitmap
                        latestBitmap = bmp
                        old?.recycle()
                        isProcessing = true
                        detector.detect(bmp)
                    }
                }
                handler.postDelayed(this, 300)
            }
        })
    }

    override fun onDetect(boxes: List<com.dicoding.skripsiapp.data.BoundingBox>, inferenceTime: Long) {
        val bmp = latestBitmap ?: run { isProcessing = false; return }
        viewModel.updateDetectionResult(boxes, inferenceTime, bmp)
        isProcessing = false
    }

    override fun onEmptyDetect() {
        binding.overlay.setClassifiedResults(emptyList())
        isProcessing = false
    }

    private fun observeUI() {
        lifecycleScope.launch {
            viewModel.classifiedBoxes.collect {
                if (it is Resource.Success)
                    binding.overlay.setClassifiedResults(it.data ?: emptyList())
            }
        }
        lifecycleScope.launch {
            viewModel.inferenceTime.collect {
                if (it is Resource.Success)
                    binding.inferenceTime.text = "${it.data} ms"
            }
        }
    }

    /**
     * Konversi YUYV (YUY2) ke Bitmap ARGB_8888.
     * Digunakan karena CameraUvcStrategy mengirim data dengan format YUYV.
     */
    private fun yuyvToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            var i = 0
            var j = 0

            while (i < pixels.size && j + 3 < data.size) {
                val y0 = data[j].toInt() and 0xFF
                val u  = (data[j + 1].toInt() and 0xFF) - 128
                val y1 = data[j + 2].toInt() and 0xFF
                val v  = (data[j + 3].toInt() and 0xFF) - 128
                pixels[i] = yuvToArgb(y0, u, v)
                if (i + 1 < pixels.size) pixels[i + 1] = yuvToArgb(y1, u, v)
                i += 2; j += 4
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            Log.e("UVC_DEBUG", "yuyvToBitmap error: ${e.message}")
            null
        }
    }

    private fun yuvToArgb(y: Int, u: Int, v: Int): Int {
        val r = (y + 1.370705f * v).toInt().coerceIn(0, 255)
        val g = (y - 0.698001f * v - 0.337633f * u).toInt().coerceIn(0, 255)
        val b = (y + 1.732446f * u).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        handler.removeCallbacksAndMessages(null)
        cameraClient?.closeCamera()
        cameraClient = null
        latestBitmap?.recycle()
        latestBitmap = null
        detector.clear()
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.dicoding.skripsiapp.USB_PERMISSION"
    }

    private fun writeLog(msg: String) {
        try {
            logFile.appendText("${java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())}: $msg\n")
        } catch (e: Exception) {}
        Log.d("UVC_DEBUG", msg)
    }
}