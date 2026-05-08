package com.dicoding.skripsiapp.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.dicoding.skripsiapp.data.DetectionResult

private val numClasses = 3

fun generateMask(
    proto: Array<Array<FloatArray>>, // [160][160][32]
    coeff: FloatArray
): Array<FloatArray> {

    val h = proto.size
    val w = proto[0].size

    val mask = Array(h) { FloatArray(w) }

    for (y in 0 until h) {
        for (x in 0 until w) {

            var sum = 0f
            for (k in coeff.indices) {
                sum += proto[y][x][k] * coeff[k]
            }

            // sigmoid
            mask[y][x] = (1f / (1f + kotlin.math.exp(-sum)))
        }
    }

    return mask
}

fun getBestDetection(
    detections: Array<FloatArray>,
    confThreshold: Float = 0.3f
): DetectionResult? {

    var bestScore = 0f
    var bestIndex = -1
    var bestClass = -1

    val numClasses = 3

    for (i in detections[0].indices) {

        var maxClassScore = 0f
        var classId = -1

        for (c in 0 until numClasses) {
            val score = detections[4 + c][i]
            if (score > maxClassScore) {
                maxClassScore = score
                classId = c
            }
        }

        if (maxClassScore > bestScore && maxClassScore > confThreshold) {
            bestScore = maxClassScore
            bestIndex = i
            bestClass = classId
        }
    }

    if (bestIndex == -1) return null

    val coeff = FloatArray(32)
    for (i in 0 until 32) {
        coeff[i] = detections[4 + numClasses + i][bestIndex]
    }

    val box = FloatArray(4)
    for (i in 0 until 4) {
        box[i] = detections[i][bestIndex]
    }

    return DetectionResult(box, coeff, bestClass, bestScore)
}

fun maskToBitmap(mask: Array<FloatArray>): Bitmap {
    val h = mask.size
    val w = mask[0].size

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

    for (y in 0 until h) {
        for (x in 0 until w) {

            val value = mask[y][x]

            if (value > 0.5f) {
                bitmap.setPixel(x, y, Color.argb(120, 0, 255, 0)) // hijau transparan
            } else {
                bitmap.setPixel(x, y, Color.TRANSPARENT)
            }
        }
    }

    return bitmap
}

fun resizeMask(maskBitmap: Bitmap, original: Bitmap): Bitmap {
    return Bitmap.createScaledBitmap(
        maskBitmap,
        original.width,
        original.height,
        true
    )
}

fun overlayMask(original: Bitmap, maskBitmap: Bitmap): Bitmap {

    val resizedMask = Bitmap.createScaledBitmap(
        maskBitmap,
        original.width,
        original.height,
        true
    )

    val result = original.copy(Bitmap.Config.ARGB_8888, true)

    val canvas = Canvas(result)
    val paint = Paint()

    canvas.drawBitmap(resizedMask, 0f, 0f, paint)

    return result
}

fun drawMaskAndBox(
    original: Bitmap,
    mask: Array<FloatArray>,
    box: FloatArray,
    classId: Int,
    score: Float
): Bitmap {

    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val paintMask = Paint().apply {
        color = getColorByClass(classId)
        style = Paint.Style.FILL
    }

    val paintBox = Paint().apply {
        color = getColorByClass(classId)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    val h = original.height
    val w = original.width

    val maskH = mask.size
    val maskW = mask[0].size

    val scaleX = w.toFloat() / maskW
    val scaleY = h.toFloat() / maskH

    // 🔥 DRAW MASK (pakai kotak biar keliatan)
    for (y in 0 until maskH) {
        for (x in 0 until maskW) {

            if (mask[y][x] > 0.3f) { // 🔥 turunin threshold

                val left = x * scaleX
                val top = y * scaleY

                canvas.drawRect(
                    left,
                    top,
                    left + scaleX,
                    top + scaleY,
                    paintMask
                )
            }
        }
    }

    // 🔲 DRAW BOX
    val cx = box[0] * w
    val cy = box[1] * h
    val bw = box[2] * w
    val bh = box[3] * h

    val left = cx - bw / 2
    val top = cy - bh / 2
    val right = cx + bw / 2
    val bottom = cy + bh / 2

    canvas.drawRect(left, top, right, bottom, paintBox)

    val label = getLabel(classId)
    val text = "$label ${"%.1f".format(score * 100)}%"

    // background biar kebaca
    val textWidth = paintText.measureText(text)
    val textHeight = paintText.textSize

    val bgPaint = Paint().apply {
        color = getColorByClass(classId)
        style = Paint.Style.FILL
    }

    val textY = if (top - 10 < 40) top + 40 else top - 10

    canvas.drawRect(
        left,
        top - textHeight - 10,
        left + textWidth + 20,
        top,
        bgPaint
    )

    // draw text
    canvas.drawText(
        text,
        left + 10,
        top - 10,
        paintText
    )

    return result
}

fun getColorByClass(classId: Int): Int {
    return when (classId) {
        0 -> Color.argb(180, 255, 0, 0)   // aedes = merah
        1 -> Color.argb(180, 0, 255, 0)   // anopheles = hijau
        2 -> Color.argb(180, 0, 0, 255)   // culex = biru
        else -> Color.argb(180, 255, 255, 0)
    }
}

fun getLabel(classId: Int): String {
    return when (classId) {
        0 -> "Aedes"
        1 -> "Anopheles"
        2 -> "Culex"
        else -> "Unknown"
    }
}
