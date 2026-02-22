package com.dicoding.skripsiapp.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Classifier(
    context: Context,
    modelPath: String,
    private val labels: List<String>
) {
    private val interpreter: Interpreter

    init {
        // Memuat model TensorFlow Lite
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(buffer)
    }

    fun classifyWithConfidence(bitmap: Bitmap): List<Pair<String, Float>> {
        val inputBuffer = preprocessImage(bitmap) // Konversi gambar ke input yang sesuai
        val outputBuffer = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, outputBuffer)

        // Cetak semua skor untuk debugging
        outputBuffer[0].forEachIndexed { index, score ->
            Log.d("Classifier", "Class: ${labels[index]}, Score: ${score * 100}%")
        }

        // Ambil semua label dengan confidence lebih dari 0.01
        val results = outputBuffer[0]
            .mapIndexed { index, score -> labels[index] to score }
            .filter { it.second > 0.001 } // Ambil kelas yang skornya > 1%

        // Jika tidak ada yang melebihi threshold, kembalikan "Tidak Diketahui"
        return if (results.isNotEmpty()) results else listOf("Tidak Diketahui" to 1.0f)
    }


    // Fungsi untuk mengklasifikasikan gambar
    fun classify(bitmap: Bitmap): String {
        Log.d("Classifier", "Starting classification for a cropped image...")

        // Preprocess gambar (ubah ke ByteBuffer)
        val inputBuffer = preprocessImage(bitmap)

        // Menyiapkan output buffer
        val outputBuffer = Array(1) { FloatArray(labels.size) }

        // Menjalankan inferensi
        interpreter.run(inputBuffer, outputBuffer)

        // Cetak skor kepercayaan untuk semua kelas
        outputBuffer[0].forEachIndexed { index, score ->
            Log.d("Classifier", "Class: ${labels[index]}, Score: $score")
        }

        // Postprocess hasil (ambil label dengan skor tertinggi)
        val maxScore = outputBuffer[0].maxOrNull() ?: 0f
        val maxIndex = outputBuffer[0].indexOfFirst { it == maxScore }
        val result = if (maxScore > 0.3) labels[maxIndex] else "Tidak Diketahui"
        Log.d("Classifier", "Max score: $maxScore, Label: ${labels[maxIndex]}")
        // Log hasil klasifikasi
        Log.d("Classifier", "Classification result: $result")

        return result
    }

    // Preprocess gambar (ubah ke ByteBuffer)
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputSize = 360 // Sesuaikan dengan ukuran input model (360x360)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 4 bytes per float, 3 channels (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[pixel++]
                val r = (pixelValue shr 16) and 0xFF // Red channel
                val g = (pixelValue shr 8) and 0xFF  // Green channel
                val b = pixelValue and 0xFF          // Blue channel

                // Masukkan ke ByteBuffer dalam urutan RGB
                byteBuffer.putFloat(r.toFloat()) // Red channel
                byteBuffer.putFloat(g.toFloat()) // Green channel
                byteBuffer.putFloat(b.toFloat()) // Blue channel
            }
        }

        return byteBuffer
    }



    companion object {
        // Fungsi untuk memuat label dari file
        fun loadLabels(context: Context, labelsPath: String): List<String> {
            return context.assets.open(labelsPath).bufferedReader().useLines { it.toList() }
        }
    }
}