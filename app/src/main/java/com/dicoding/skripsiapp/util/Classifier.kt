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
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(buffer)

        val inputShape = interpreter.getInputTensor(0).shape()
        Log.d("Classifier", "Input shape: ${inputShape.contentToString()}")
    }

    fun classifyWithConfidence(bitmap: Bitmap): List<Pair<String, Float>> {
        val inputBuffer = preprocessImage(bitmap)
        val outputBuffer = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, outputBuffer)

        // Terapkan softmax karena output adalah logits mentah
        val probabilities = softmax(outputBuffer[0])

        probabilities.forEachIndexed { index, score ->
            Log.d("Classifier_SOFTMAX", "Class: ${labels[index]}, Score: ${score * 100}%")
        }

        val results = probabilities
            .mapIndexed { index, score -> labels[index] to score }
            .filter { it.second > 0.001f }

        return if (results.isNotEmpty()) results else listOf("Tidak Diketahui" to 1.0f)
    }

    fun classify(bitmap: Bitmap): String {
        Log.d("Classifier", "Starting classification...")

        val inputBuffer = preprocessImage(bitmap)
        val outputBuffer = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, outputBuffer)

        // Terapkan softmax
        val probabilities = softmax(outputBuffer[0])

        probabilities.forEachIndexed { index, score ->
            Log.d("Classifier", "Class: ${labels[index]}, Score: ${score * 100}%")
        }

        val maxScore = probabilities.maxOrNull() ?: 0f
        val maxIndex = probabilities.indexOfFirst { it == maxScore }
        val result = if (maxScore > 0.3f) labels[maxIndex] else "Tidak Diketahui"

        Log.d("Classifier", "Result: $result (${"%.2f".format(maxScore * 100)}%)")
        return result
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = logits.map { Math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // ✅ shape = [1, 3, 256, 256] — channel first
        // shape()[1] = 3 (channel), shape()[2] = 256 (height), shape()[3] = 256 (width)
        val inputTensor = interpreter.getInputTensor(0)
        val shape = inputTensor.shape()

        // ✅ Deteksi format otomatis
        val isChannelFirst = shape[1] == 3 || shape[1] == 1  // NCHW
        val inputSize = if (isChannelFirst) shape[2] else shape[1]  // ambil H

        Log.d("Classifier", "Format: ${if (isChannelFirst) "NCHW" else "NHWC"}, inputSize: $inputSize")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * 3 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        if (isChannelFirst) {
            // ✅ NCHW: isi semua R dulu, lalu G, lalu B
            for (pixel in intValues) byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            for (pixel in intValues) byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)  // G
            for (pixel in intValues) byteBuffer.putFloat((pixel and 0xFF) / 255f)           // B
        } else {
            // NHWC: R G B per pixel
            for (pixel in intValues) {
                byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                byteBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }

        return byteBuffer
    }

    companion object {
        fun loadLabels(context: Context, labelsPath: String): List<String> {
            return context.assets.open(labelsPath).bufferedReader().useLines { it.toList() }
        }
    }
}