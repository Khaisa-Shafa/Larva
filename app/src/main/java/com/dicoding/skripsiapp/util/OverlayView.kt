package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.BoundingBox
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel
import com.dicoding.skripsiapp.viewmodel.LiveDetectionViewModel.ClassifiedBox

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<LiveDetectionViewModel.ClassifiedBox> = emptyList()

    fun setClassifiedResults(list: List<LiveDetectionViewModel.ClassifiedBox>) {
        results = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val boxPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }

        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 40f
        }

        results.forEach {
            val box = it.boundingBox

            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height

            canvas.drawRect(left, top, right, bottom, boxPaint)

            // 🔥 LABEL FINAL (MobileViT)
            canvas.drawText(
                "${it.label} (${(it.confidence * 100).toInt()}%)",
                left,
                top - 10,
                textPaint
            )
        }
    }
}