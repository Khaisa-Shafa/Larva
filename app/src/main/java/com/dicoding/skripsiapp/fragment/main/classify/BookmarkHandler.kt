package com.dicoding.skripsiapp.fragment.main.classify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dicoding.skripsiapp.data.BookmarkItem
import com.dicoding.skripsiapp.data.LocationData
import com.dicoding.skripsiapp.databinding.FragmentPageDetectionBinding
import com.dicoding.skripsiapp.util.Resource
import com.dicoding.skripsiapp.viewmodel.PageDetectionViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class BookmarkHandler(
    private val fragment: Fragment,
    private val viewModel: PageDetectionViewModel,
    private val binding: FragmentPageDetectionBinding,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    fun handleBookmark() {
        if (binding == null) return
        showBookmarkLoading(true)
        binding.ivBookmark.isEnabled = false
        val currentBitmapResource = viewModel.annotatedBitmap.value
        val currentPrediction = viewModel.predictionResult.value

        if (currentBitmapResource !is Resource.Success) {
            Toast.makeText(
                fragment.requireContext(),
                "Belum ada gambar dengan bounding box",
                Toast.LENGTH_SHORT
            ).show()
            resetBookmarkState()
            return
        }

        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            resetBookmarkState()
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(fragment.requireContext(), "Tidak bisa bookmark, nyalakan GPS terlebih dahulu!", Toast.LENGTH_SHORT).show()
            resetBookmarkState()
            return
        }

        fragment.lifecycleScope.launch {
            val locationResult = getLastKnownLocation()
            if (locationResult == null) {
                Toast.makeText(fragment.requireContext(), "Kota/Provinsi tidak ditemukan!", Toast.LENGTH_SHORT).show()
                resetBookmarkState()
                return@launch
            }

            val bitmap = currentBitmapResource.data
            val imageUri = bitmap?.let { viewModel.uploadBitmapToFirebase(it) }

            if (imageUri == null) {
                Toast.makeText(fragment.requireContext(), "Tidak dapat dibookmark", Toast.LENGTH_SHORT).show()
                resetBookmarkState()
                return@launch
            }

            val isBookmarked = viewModel.bookmarks.value?.any { it.imageUri == imageUri } == true

            if (isBookmarked) {
                viewModel.removeBookmarkByUri(imageUri)
                Toast.makeText(fragment.requireContext(), "Bookmark dihapus", Toast.LENGTH_SHORT).show()
            } else {
                val boundingBoxes = (currentPrediction as? Resource.Success)?.data
                if (boundingBoxes.isNullOrEmpty()) {
                    Toast.makeText(
                        fragment.requireContext(),
                        "Tidak ada objek terdeteksi",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetBookmarkState()
                    return@launch
                }

                val detectedObjects = boundingBoxes.joinToString("\n") {
                    "${it.clsName}: ${String.format("%.2f", it.cnf * 100)}%"
                }

                val classificationResults = (viewModel.classificationResult.value as? Resource.Success)?.data ?: "Hasil klasifikasi tidak ditemukan"


                val topDetected = boundingBoxes.maxByOrNull { it.cnf }?.clsName ?: "Unknown"

                if (topDetected != "Aedes" && topDetected != "Culex") {
                    Toast.makeText(
                        fragment.requireContext(),
                        "Tidak dapat bookmark karena hasil deteksi bukan Aedes atau Culex",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetBookmarkState()
                    return@launch
                }

                val bookmarkItem = BookmarkItem(
                    imageUri = imageUri,
                    topClassification = topDetected,
                    fullClassificationResults = classificationResults,
                    isBookmarked = true,
                    type = "Detection",
                    date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                    latitude = locationResult.latitude,
                    longitude = locationResult.longitude,
                    city = locationResult.city ?: "Kota tidak ditemukan!",
                    province = locationResult.province ?: "Provinsi tidak ditemukan!",
                )
                viewModel.addBookmark(bookmarkItem)
                Toast.makeText(fragment.requireContext(), "Berhasil menyimpan ke bookmark", Toast.LENGTH_SHORT).show()
            }

            resetBookmarkState()
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermission = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            Log.i("Permission", "Permission granted")
        } else {
            Log.e("Permission", "Permission denied")
            Toast.makeText(fragment.requireContext(), "Permission Denied!! Try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = fragment.requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private suspend fun getLastKnownLocation(): LocationData? {
        return suspendCancellableCoroutine { continuation ->
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fragment.lifecycleScope.launch {
                        val address = getAddressFromCoordinates(location.latitude, location.longitude)
                        continuation.resume(
                            LocationData(location.latitude, location.longitude, address.first, address.second)
                        )
                    }
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }

    private suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(fragment.requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addresses?.firstOrNull()?.locality ?: "Kota tidak ditemukan!"
                val province = addresses?.firstOrNull()?.adminArea ?: "Provinsi tidak ditemukan!"
                Pair(city, province)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair("Kota tidak ditemukan!", "Provinsi tidak ditemukan!")
            }
        }
    }

    private fun resetBookmarkState() {
        binding.ivBookmark.postDelayed({
            if (binding != null) {
                binding.ivBookmark.isEnabled = true
                showBookmarkLoading(false)
            }
        }, 1000)
    }

    private fun showBookmarkLoading(isLoading: Boolean) {
        binding.progressbar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.ivBookmark.alpha = if (isLoading) 0.5f else 1f
    }
}

