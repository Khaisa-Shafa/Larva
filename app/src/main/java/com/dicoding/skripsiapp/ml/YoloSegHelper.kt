package com.dicoding.skripsiapp.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.dicoding.skripsiapp.data.YoloSegResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class YoloSegHelper(
    context: Context
) {

    private val interpreter: Interpreter

    private val inputSize = 640
    private val numElements = 39
    private val numAnchors = 8400

    init {
        val model = loadModelFile(context, "baru/yolo11_seg.tflite")
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun detect(bitmap: Bitmap): YoloSegResult {

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputBuffer =
            ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((pixel and 0xFF) / 255f)
        }

        val output0 = Array(1) { Array(numElements) { FloatArray(numAnchors) } }
        val output1 = Array(1) { Array(160) { Array(160) { FloatArray(32) } } }

        val outputs = mapOf(
            0 to output0,
            1 to output1
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        Log.d("YOLO", "Sample pixel: ${pixels[0]}")
        
        return YoloSegResult(
            detections = output0[0],
            proto = output1[0]
        )


    }


}