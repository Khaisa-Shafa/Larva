package com.dicoding.skripsiapp.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.IOException

object ImageUtils {

    fun correctBitmapOrientation(context: Context, bitmap: Bitmap, imageUri: Uri?): Bitmap {
        if (imageUri == null) return bitmap
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: IOException) {
            Log.e("ImageUtils", "Error reading EXIF data: ${e.message}", e)
            bitmap
        }
    }

    fun validateBitmap(bitmap: Bitmap?): Boolean {
        if (bitmap == null) {
            Log.e("ImageUtils", "Bitmap is null")
            return false
        }
        if (bitmap.width == 0 || bitmap.height == 0) {
            Log.e("ImageUtils", "Bitmap dimensions are invalid")
            return false
        }
        return true
    }

    fun downloadImage(context: Context, mBitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Larvae_Images_${System.currentTimeMillis() / 1000}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.let { uri ->
            context.contentResolver.insert(uri, contentValues)?.also { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    if (!mBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        Toast.makeText(context, "Couldn't save the bitmap", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Image Saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}