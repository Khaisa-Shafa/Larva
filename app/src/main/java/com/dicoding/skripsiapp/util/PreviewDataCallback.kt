package com.dicoding.skripsiapp.util

//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.ImageFormat
//import android.graphics.Rect
//import android.graphics.YuvImage
//import android.os.Build
//import java.io.ByteArrayOutputStream
//import java.nio.ByteBuffer
//
//class PreviewDataCallback : IPreviewDataCallBack, com.jiangdg.ausbc.callback.IPreviewDataCallBack {
//    private var detector: Detector? = null
//    var onBitmapReady: ((Bitmap) -> Unit)? = null
//
//    fun setDetector(detector: Detector) {
//        this.detector = detector
//    }
//
//    override fun onPreviewDataReceived(data: ByteArray, width: Int, height: Int, format: Int) {
//        // Konversi data preview ke Bitmap
//        val bitmap = previewDataToBitmap(data, width, height, format)
//
//        onBitmapReady?.invoke(bitmap)
//        detector?.detect(bitmap)
//
//        // Lakukan deteksi objek
//        detector?.detect(bitmap)
//    }
//
//    override fun onPreviewData(
//        data: ByteArray?,
//        width: Int,
//        height: Int,
//        format: com.jiangdg.ausbc.callback.IPreviewDataCallBack.DataFormat,
//    ) {
//        if (data == null) return
//
//        // Konversi data preview ke Bitmap berdasarkan format
//        val bitmap = when (format) {
//            com.jiangdg.ausbc.callback.IPreviewDataCallBack.DataFormat.NV21 -> {
//                val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
//                val outputStream = ByteArrayOutputStream()
//                yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
//                val jpegData = outputStream.toByteArray()
//                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
//            }
//            com.jiangdg.ausbc.callback.IPreviewDataCallBack.DataFormat.RGBA -> {
//                val bitmapConfig = when {
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Bitmap.Config.RGBA_1010102
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Bitmap.Config.RGBA_F16
//                    else -> Bitmap.Config.ARGB_8888
//                }
//
//                Bitmap.createBitmap(width, height, bitmapConfig).apply {
//                    copyPixelsFromBuffer(ByteBuffer.wrap(data))
//                }
//            }
//            else -> throw IllegalArgumentException("Unsupported preview format: $format")
//        }
//
//        // Lakukan deteksi objek
//        detector?.detect(bitmap)
//    }
//
//    private fun previewDataToBitmap(data: ByteArray, width: Int, height: Int, format: Int): Bitmap {
//        // Sesuaikan dengan format data preview (misalnya, YUV, NV21, atau RGB)
//        return when (format) {
//            ImageFormat.NV21 -> {
//                val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
//                val outputStream = ByteArrayOutputStream()
//                yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
//                val jpegData = outputStream.toByteArray()
//                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
//            }
//            ImageFormat.RGB_565 -> {
//                Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
//                    copyPixelsFromBuffer(ByteBuffer.wrap(data))
//                }
//            }
//            else -> throw IllegalArgumentException("Unsupported preview format: $format")
//        }
//    }
//}