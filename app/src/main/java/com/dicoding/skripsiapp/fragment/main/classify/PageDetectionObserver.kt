package com.dicoding.skripsiapp.fragment.main.classify

import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.FragmentPageDetectionBinding
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_CLASSIFICATION
import com.dicoding.skripsiapp.util.Constants.LABELS_PATH_DETECTION
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_CLASSIFICATION
import com.dicoding.skripsiapp.util.Constants.MODEL_PATH_DETECTION
import com.dicoding.skripsiapp.util.DialogUtilsPrediction
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PageDetectionObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val pageDetectionViewModel: PageDetectionViewModel,
    private val binding: FragmentPageDetectionBinding,
    private val showLoading: (Boolean) -> Unit,
    private val showToast: (String) -> Unit
) {
    fun setupObservers() {
        pageDetectionViewModel.initializeDetector(
            binding.root.context,
            MODEL_PATH_DETECTION,
            LABELS_PATH_DETECTION
        )

        pageDetectionViewModel.initializeClassifier(
            binding.root.context,
            MODEL_PATH_CLASSIFICATION,
            LABELS_PATH_CLASSIFICATION
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycleScope.launchWhenStarted {
                launch {
                    pageDetectionViewModel.annotatedBitmap.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> showLoading(true)
                            is Resource.Success -> {
                                showLoading(false)
                                val bitmap = resource.data
                                binding.imageVieww.setImageBitmap(bitmap)
                                if (bitmap != null) {
                                    binding.ivBookmark.setImageResource(R.drawable.ic_bookmarked)
                                    Log.d("PredictionActivity", "Bitmap successfully loaded into ImageView.")
                                } else {
                                    Log.d("PredictionActivity", "Bitmap is null.")
                                }
                            }
                            is Resource.Error -> {
                                showLoading(false)
                                Log.e("PredictionActivity", "Failed to load Bitmap: ${resource.message}")
                                showToast("Failed to load image: ${resource.message}")
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.predictionResult.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> showLoading(true)
                            is Resource.Success -> {
                                showLoading(false)
                                val result = resource.data.orEmpty()

                                val predictionText = if (result.isNotEmpty()) {
                                    result.joinToString("\n") { "${it.clsName}: ${it.cnf * 100}%" }
                                } else {
                                    "No objects detected"
                                }
                                binding.tvPrediction.text = predictionText

                                val topClass = result.maxByOrNull { it.cnf }?.clsName
                                updateFunFactText(topClass)

                                val drawable = binding.imageVieww.drawable
                                if (drawable != null) {
                                    val originalBitmap = drawable.toBitmap()
                                    pageDetectionViewModel.overlayBoundingBoxes(originalBitmap, result)
                                    pageDetectionViewModel.classifyCroppedImages(originalBitmap, result)
                                }
                            }
                            is Resource.Error -> {
                                showLoading(false)
                                showToast("Error detecting objects: ${resource.message}")
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.classificationResult.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> showLoading(true)
                            is Resource.Success -> {
                                showLoading(false)
                                val result = resource.data
                                Log.d("PageDetection", "Classification Results: \n$result")
                                binding.tvPrediction.text = result
                            }
                            is Resource.Error -> {
                                showLoading(false)
                                Log.e("PageDetection", "Error during classification: ${resource.message}")
                                showToast("Error during classification: ${resource.message}")
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.funFact.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                showLoading(true)
                                binding.tvFunfact.text = "Fetching fun fact..."
                            }
                            is Resource.Success -> {
                                showLoading(false)
                                resource.data?.let { funFact ->
                                    DialogUtilsPrediction.showFunFactDialog(binding.root.context, funFact) {
                                        binding.tvFunfact.text = "Tap for a fun fact!"
                                    }
                                }
                            }
                            is Resource.Error -> {
                                showLoading(false)
                                binding.tvFunfact.text = "Failed to fetch fun fact: ${resource.message}"
                                showToast("Could not fetch fun fact. Please try again.")
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.toastMessage.collect { event ->
                        event?.getContentIfNotHandled()?.let { message ->
                            Log.d("ToastDebug", "Toast received: $message")
                            showToast(message)
                        } ?: Log.d("ToastDebug", "Toast event was null or already handled")
                    }
                }

                launch {
                    pageDetectionViewModel.inferenceTime.collect { resource ->
                        if (resource is Resource.Success) {
                            Log.d("PredictionActivity", "Inference time received: ${resource.data}ms")
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.errorMessage.collect { resource ->
                        if (resource is Resource.Error) {
                            showLoading(false)
                            showToast(resource.message ?: "Unknown error")
                        }
                    }
                }

                launch {
                    pageDetectionViewModel.bookmarks.observe(lifecycleOwner) { bookmarkList ->
                        val currentBitmapResource = pageDetectionViewModel.annotatedBitmap.value
                        if (currentBitmapResource is Resource.Success) {
                            lifecycleOwner.lifecycleScope.launch {
                                delay(500)
                                val bitmap = currentBitmapResource.data
                                val imageUri = bitmap?.let { pageDetectionViewModel.uploadBitmapToFirebase(it) }
                                val isBookmarked = bookmarkList.any { it.imageUri == imageUri }
                                Log.d("Bookmark", "Observer: Image URI = $imageUri, Bookmarked = $isBookmarked")
                                binding.ivBookmark.setImageResource(
                                    if (isBookmarked) R.drawable.ic_bookmarked_filled else R.drawable.ic_bookmarked
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateFunFactText(topClass: String?) {
        if (topClass.equals("Aedes", ignoreCase = true) || topClass.equals(
                "Culex",
                ignoreCase = true
            )
        ) {
            binding.tvFunfact.visibility = View.VISIBLE
            binding.tvFunfact.text = "Tap for a fun fact about $topClass!"
        } else {
            binding.tvFunfact.visibility = View.GONE
        }
    }
}
