package com.dicoding.skripsiapp.util

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class CameraHelper(private val fragment: Fragment) {

    private val takePictureLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                onImageCaptured?.invoke(cameraImageUri)
            } else {
                Toast.makeText(fragment.requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }

    private var cameraImageUri: Uri? = null
    var onImageCaptured: ((Uri?) -> Unit)? = null

    fun captureImageFromCamera() {
        val tempFile = File.createTempFile("camera_image", ".jpg", fragment.requireContext().cacheDir).apply {
            deleteOnExit()
        }
        cameraImageUri = FileProvider.getUriForFile(
            fragment.requireContext(),
            "${fragment.requireContext().packageName}.fileprovider",
            tempFile
        )
        takePictureLauncher.launch(cameraImageUri!!)
    }
}
