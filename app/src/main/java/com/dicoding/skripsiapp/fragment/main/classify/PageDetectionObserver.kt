package com.dicoding.skripsiapp.fragment.main.classify

import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.FragmentPageDetectionBinding
import com.dicoding.skripsiapp.util.DialogUtilsPrediction
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dicoding.skripsiapp.util.getModelConfig

class PageDetectionObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val selectedModel: Int,
    private val pageDetectionViewModel: PageDetectionViewModel,
    private val binding: FragmentPageDetectionBinding,
    private val showLoading: (Boolean) -> Unit,
    private val showToast: (String) -> Unit

) {
    fun setupObservers() {
        val config = getModelConfig(selectedModel)

        pageDetectionViewModel.initializeDetector(
            binding.root.context,
            config.detectionModel,
            config.detectionLabels
        )

        pageDetectionViewModel.initializeClassifier(
            binding.root.context,
            config.classificationModel,
            config.classificationLabels
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
                                if (result.isEmpty()) {
                                    binding.tvPrediction.text = "Tidak ada objek terdeteksi"
                                    return@collect
                                }

                                val predictionText = if (result.isNotEmpty()) {
                                    result.joinToString("\n") { box ->
                                        val conf = box.cnf.coerceIn(0f, 1f)
                                        "${box.clsName}: ${String.format("%.2f", conf * 100)}%"
                                    }
                                } else {
                                    "No objects detected"
                                }
                                binding.tvPrediction.text = predictionText

                                val drawable = binding.imageVieww.drawable
                                if (drawable != null) {
                                    val originalBitmap = drawable.toBitmap()
                                    //pageDetectionViewModel.overlayBoundingBoxes(originalBitmap, result)
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
}
