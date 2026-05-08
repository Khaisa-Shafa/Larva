package com.dicoding.skripsiapp.fragment.main.classify

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.databinding.FragmentPageDetectionBinding
import com.dicoding.skripsiapp.ml.YoloSegHelper
import com.dicoding.skripsiapp.ml.drawMaskAndBox
import com.dicoding.skripsiapp.ml.generateMask
import com.dicoding.skripsiapp.ml.getBestDetection
import com.dicoding.skripsiapp.ml.overlayMask
import com.dicoding.skripsiapp.ml.maskToBitmap
import com.dicoding.skripsiapp.ml.resizeMask
import com.dicoding.skripsiapp.util.Classifier
import com.dicoding.skripsiapp.util.Constants
import com.dicoding.skripsiapp.util.Detector
import com.dicoding.skripsiapp.util.DialogUtilsPrediction
import com.dicoding.skripsiapp.util.getModelConfig
import com.dicoding.skripsiapp.util.hideBottomNavigationView
import com.dicoding.skripsiapp.viewmodel.DetectionViewPagerViewModel
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class PageDetectionFragment : Fragment() {

    private var _binding: FragmentPageDetectionBinding? = null
    private val binding get() = _binding!!

    private val bookmarkViewModel: DetectionViewPagerViewModel by viewModels()
    private val pageDetectionViewModel: PageDetectionViewModel by viewModels()

    private var selectedModel: Int = Constants.MODEL_YOLO11_MOBILEVIT
    private var currentBitmap: Bitmap? = null
    private var currentPrediction: String = ""
    private var isBookmarked = false

    // Untuk YOLOv8
    private var detector: Detector? = null
    private var classifier: Classifier? = null

    // Untuk YOLO11-Seg
    private var segHelper: YoloSegHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideBottomNavigationView()
        _binding = FragmentPageDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil model yang dipilih dari Bundle
        selectedModel = arguments?.getInt("selected_model", Constants.MODEL_YOLO11_MOBILEVIT)
            ?: Constants.MODEL_YOLO11_MOBILEVIT

        initModels()
        setupWindowInsets()
        setupUI()
    }

    private fun initModels() {
        val config = getModelConfig(selectedModel)

        if (selectedModel == Constants.MODEL_YOLOV8_MOBILENETV3) {
            // YOLOv8 — pakai Detector biasa (bounding box, tanpa segmentasi)
            detector = Detector(
                requireContext(),
                config.detectionModel,
                config.detectionLabels,
                object : Detector.DetectorListener {
                    override fun onEmptyDetect() {
                        binding.tvPrediction.text = "Tidak ada objek terdeteksi"
                    }
                    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                        // Tidak dipakai di sini, proses di runDetection()
                    }
                }
            ).also { it.setup() }

            val labels = Classifier.loadLabels(requireContext(), config.classificationLabels)
            classifier = Classifier(requireContext(), config.classificationModel, labels)

        } else {
            // YOLO11-Seg — pakai YoloSegHelper (segmentasi)
            segHelper = YoloSegHelper(requireContext())
        }
    }

    private fun setupUI() {
        // Tampilkan label model di toolbar
//        binding.txtPrediction.text = if (selectedModel == Constants.MODEL_YOLOV8_MOBILENETV3)
//            "Prediction" else "Prediction (YOLO11-Seg)"
        binding.txtPrediction.text = "Prediction"

        binding.apply {

            btnDoDetectino.setOnClickListener {
                selectImageLauncher.launch("image/*")
            }

            btnResetImage.setOnClickListener {
                imageVieww.setImageResource(R.drawable.logo_detection)
                tvPrediction.text = ""
                currentBitmap = null
                currentPrediction = ""
                isBookmarked = false
                ivBookmark.setImageResource(R.drawable.ic_bookmarked)
                showToast("Reset berhasil")
            }

            ivBack.setOnClickListener {
                findNavController().navigateUp()
            }

            ivMore.setOnClickListener {
                val popup = PopupMenu(requireContext(), ivMore)
                popup.menuInflater.inflate(R.menu.popup_menu_detection, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menuModelResult -> {
                            val metrics = pageDetectionViewModel.loadTrainingMetrics(requireContext())
                            DialogUtilsPrediction.showTrainingMetricsTableDialog(requireContext(), metrics)
                            true
                        }
                        R.id.menuConfusionMatrix -> {
                            val (matrix, classes) = pageDetectionViewModel.loadConfusionMatrix(requireContext())
                            DialogUtilsPrediction.showConfusionMatrixDialog(requireContext(), matrix, classes)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            ivBookmark.setOnClickListener {
                if (currentBitmap == null || currentPrediction.isEmpty()) {
                    showToast("Lakukan deteksi terlebih dahulu!")
                    return@setOnClickListener
                }
                if (isBookmarked) {
                    showToast("Sudah disimpan!")
                    return@setOnClickListener
                }
                saveBookmark()
            }
        }
    }

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processImage(it) }
        }

    private fun processImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            currentBitmap = bitmap
            binding.imageVieww.setImageBitmap(bitmap)

            if (selectedModel == Constants.MODEL_YOLOV8_MOBILENETV3) {
                runYoloV8Detection(bitmap)
            } else {
                runYolo11Segmentation(bitmap)
            }
        } catch (e: Exception) {
            Log.e("Detection", "Error load image", e)
            showToast("Gagal memuat gambar")
        }
    }

    // ===== YOLOv8 + MobileNetV3 — TANPA segmentasi =====
    private fun runYoloV8Detection(bitmap: Bitmap) {
        val det = detector ?: return
        val cls = classifier ?: return

        binding.progressbar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                // 1. Deteksi bounding box
                val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                val boxes = det.detectSync(resized) // ⚠️ lihat catatan di bawah

                if (boxes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressbar.visibility = View.GONE
                        binding.tvPrediction.text = "Tidak ada objek terdeteksi"
                    }
                    return@launch
                }

                // 2. Klasifikasi setiap box
                val results = StringBuilder()
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(mutableBitmap)
                val boxPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    strokeWidth = 5f
                    style = android.graphics.Paint.Style.STROKE
                }
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = bitmap.width * 0.04f
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                }

                boxes.forEachIndexed { idx, box ->
                    // Crop dan klasifikasi
                    val x = (box.x1 * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val y = (box.y1 * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                    val w = ((box.x2 - box.x1) * bitmap.width).toInt().coerceAtLeast(1)
                    val h = ((box.y2 - box.y1) * bitmap.height).toInt().coerceAtLeast(1)
                    val crop = Bitmap.createBitmap(bitmap, x, y, w.coerceAtMost(bitmap.width - x), h.coerceAtMost(bitmap.height - y))

                    val classResult = cls.classifyWithConfidence(crop)
                    val best = classResult.maxByOrNull { it.second }
                    val label = best?.first ?: "Unknown"
                    val conf = best?.second ?: 0f

                    // Gambar bounding box (tanpa mask/segmentasi)
                    val bx1 = box.x1 * bitmap.width
                    val by1 = box.y1 * bitmap.height
                    val bx2 = box.x2 * bitmap.width
                    val by2 = box.y2 * bitmap.height
                    canvas.drawRect(bx1, by1, bx2, by2, boxPaint)
                    canvas.drawText(
                        "$label ${"%.1f".format(conf * 100)}%",
                        bx1, maxOf(by1 - 8, textPaint.textSize),
                        textPaint
                    )

                    results.append("${idx + 1}. $label (${"%.1f".format(conf * 100)}%)\n")
                }

                currentPrediction = results.toString().trim()
                currentBitmap = mutableBitmap

                withContext(Dispatchers.Main) {
                    binding.progressbar.visibility = View.GONE
                    binding.imageVieww.setImageBitmap(mutableBitmap)
                    binding.tvPrediction.text = currentPrediction
                }

            } catch (e: Exception) {
                Log.e("Detection", "Error YOLOv8: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.progressbar.visibility = View.GONE
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    // ===== YOLO11-Seg + MobileViT — DENGAN segmentasi =====
    private fun runYolo11Segmentation(bitmap: Bitmap) {
        val seg = segHelper ?: return

        binding.progressbar.visibility = View.VISIBLE

        try {
            val result = seg.detect(bitmap)
            val best = getBestDetection(result.detections)

            if (best != null) {
                val mask = generateMask(result.proto, best.coeff)
                val finalImage = drawMaskAndBox(bitmap, mask, best.box, best.classId, best.score)

                currentBitmap = finalImage
                currentPrediction = when (best.classId) {
                    0 -> "Aedes"
                    1 -> "Anopheles"
                    2 -> "Culex"
                    else -> "Unknown"
                }

                binding.progressbar.visibility = View.GONE
                binding.imageVieww.setImageBitmap(finalImage)
                binding.tvPrediction.text = "$currentPrediction (${"%.1f".format(best.score * 100)}%)"

            } else {
                binding.progressbar.visibility = View.GONE
                binding.tvPrediction.text = "Tidak ada objek terdeteksi"
            }
        } catch (e: Exception) {
            Log.e("Detection", "Error YOLO11-Seg: ${e.message}", e)
            binding.progressbar.visibility = View.GONE
            showToast("Error: ${e.message}")
        }
    }

    private fun saveBookmark() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showToast("Login terlebih dahulu!")
            return
        }

        val fileName = "detection_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(requireContext().filesDir, fileName)
        try {
            java.io.FileOutputStream(file).use { out ->
                currentBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            showToast("Gagal menyimpan gambar")
            return
        }

        val modelName = if (selectedModel == Constants.MODEL_YOLOV8_MOBILENETV3)
            "YOLOv8 + MobileNetV3" else "YOLO11-Seg + MobileViT"

        val bookmark = BookmarkItem(
            id = UUID.randomUUID().toString(),
            imageUri = Uri.fromFile(file).toString(),
            topClassification = currentPrediction,
            fullClassificationResults = "$modelName",
            date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
            type = "Detection",
            isBookmarked = true,
            province = "",
            city = "",
            latitude = 0.0,
            longitude = 0.0
        )

        viewLifecycleOwner.lifecycleScope.launch {
            bookmarkViewModel.saveBookmark(bookmark, userId)
            isBookmarked = true
            binding.ivBookmark.setImageResource(R.drawable.ic_bookmarked_filled)
            showToast("Berhasil disimpan!")
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector?.clear()
        _binding = null
    }
}