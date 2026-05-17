package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<LiveDetectionViewModel.ClassifiedBox> = emptyList()

    // ✅ TAMBAHAN: untuk render mask segmentasi
    private var maskBitmap: Bitmap? = null

    // ✅ TAMBAHAN: dipanggil dari onDetect setelah drawMaskAndBox()
    fun setMaskBitmap(bitmap: Bitmap?) {
        maskBitmap = bitmap
        invalidate()
    }

    fun setClassifiedResults(list: List<LiveDetectionViewModel.ClassifiedBox>) {
        results = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Layer 1: mask bitmap
        maskBitmap?.let { bmp ->
            val scaled = Bitmap.createScaledBitmap(bmp, width, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
        }

        // ✅ Hanya 1 box terbaik — TIDAK ADA results.forEach di sini
        val best = results.maxByOrNull { it.confidence } ?: return

        val boxPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        val textBgPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            isFakeBoldText = true
        }

        val left = best.boundingBox.x1 * width
        val top = best.boundingBox.y1 * height
        val right = best.boundingBox.x2 * width
        val bottom = best.boundingBox.y2 * height

        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Background label supaya terbaca
        val label = "${best.label} ${"%.1f".format(best.confidence * 100)}%"
        val textWidth = textPaint.measureText(label)
        canvas.drawRect(left, top - 60f, left + textWidth + 16f, top, textBgPaint)
        canvas.drawText(label, left + 8f, top - 14f, textPaint)
    }
}