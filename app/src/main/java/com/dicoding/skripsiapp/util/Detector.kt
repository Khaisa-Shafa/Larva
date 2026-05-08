package com.dicoding.skripsiapp.util

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.dicoding.skripsiapp.data.BoundingBox
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private var detectorListener: DetectorListener
): LifecycleObserver {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0


    fun setListener(listener: DetectorListener) {
        this.detectorListener = listener
    }


    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    /**
     * Called when the lifecycle owner is in the ON_CREATE state.
     * Initialize the TFLite interpreter and other resources.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        options.setNumThreads(4)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]

        val output0Shape = interpreter?.getOutputTensor(0)?.shape() ?: return
        Log.d("Detector", "Output0 shape: ${output0Shape.contentToString()}")

        // Dinamis, tidak hardcode
        numChannel = output0Shape[1]
        numElements = output0Shape[2]

        Log.d("Detector", "numChannel: $numChannel, numElements: $numElements")

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Called when the lifecycle owner is in the ON_DESTROY state.
     * Release the TFLite interpreter and other resources.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        interpreter ?: return

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val inputBuffer = ByteBuffer.allocateDirect(1 * tensorWidth * tensorHeight * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(tensorWidth * tensorHeight)
        resizedBitmap.getPixels(pixels, 0, tensorWidth, 0, 0, tensorWidth, tensorHeight)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((pixel and 0xFF) / 255f)
        }

        // Dinamis berdasarkan numChannel dan numElements
        val output0 = Array(1) { Array(numChannel) { FloatArray(numElements) } }

        // Output ke-2 hanya ada di YOLO11-Seg, cek dulu jumlah output tensor
        val outputCount = interpreter?.outputTensorCount ?: 1

        val startTime = SystemClock.uptimeMillis()

        if (outputCount > 1) {
            val output1Shape = interpreter?.getOutputTensor(1)?.shape()
            Log.d("Detector", "Output1 shape: ${output1Shape?.contentToString()}")
            val output1 = Array(1) { Array(output1Shape!![1]) { Array(output1Shape[2]) { FloatArray(output1Shape[3]) } } }
            val outputs = mapOf(0 to output0, 1 to output1)
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        } else {
            val outputs = mapOf(0 to output0)
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        }

        val inferenceTime = SystemClock.uptimeMillis() - startTime

        // Flatten output0
        val flatOutput = FloatArray(numChannel * numElements)
        var index = 0
        for (i in 0 until numChannel) {
            for (j in 0 until numElements) {
                flatOutput[index++] = output0[0][i][j]
            }
        }

        val bestBoxes = bestBox(flatOutput)
        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        }

        detectorListener.onDetect(bestBoxes, inferenceTime)
    }

    private fun bestBox(array: FloatArray) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < 4 + labels.size){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    fun detectSync(frame: Bitmap): List<BoundingBox> {
        interpreter ?: return emptyList()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)
        val inputBuffer = ByteBuffer.allocateDirect(1 * tensorWidth * tensorHeight * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(tensorWidth * tensorHeight)
        resizedBitmap.getPixels(pixels, 0, tensorWidth, 0, 0, tensorWidth, tensorHeight)
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((pixel and 0xFF) / 255f)
        }

        val output0 = Array(1) { Array(numChannel) { FloatArray(numElements) } }
        val outputs = mapOf(0 to output0)
        interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        val flatOutput = FloatArray(numChannel * numElements)
        var index = 0
        for (i in 0 until numChannel) {
            for (j in 0 until numElements) {
                flatOutput[index++] = output0[0][i][j]
            }
        }

        return bestBox(flatOutput) ?: emptyList()
    }

    interface DetectorListener {

        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.01F
        private const val IOU_THRESHOLD = 0.2F
    }
}