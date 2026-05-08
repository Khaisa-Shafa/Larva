package com.dicoding.skripsiapp.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.dicoding.skripsiapp.R
import org.json.JSONObject

object DialogUtilsPrediction {

    fun showTrainingMetricsTableDialog(context: Context, metrics: JSONObject) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confusion_matrix, null)
        val tableLayout = dialogView.findViewById<TableLayout>(R.id.tableLayout)

        // ⚠️ Fix key dari (M) ke (B)
        val data = linkedMapOf(
            "Epoch"                to metrics.optInt("epoch").toString(),
            "Train Box Loss"       to metrics.optDouble("train/box_loss").format(),
            "Train Cls Loss"       to metrics.optDouble("train/cls_loss").format(),
            "Train Dfl Loss"       to metrics.optDouble("train/dfl_loss").format(),
            "Val Box Loss"         to metrics.optDouble("val/box_loss").format(),
            "Val Cls Loss"         to metrics.optDouble("val/cls_loss").format(),
            "Val Dfl Loss"         to metrics.optDouble("val/dfl_loss").format(),
            "Precision"            to metrics.optDouble("metrics/precision(B)").format(),
            "Recall"               to metrics.optDouble("metrics/recall(B)").format(),
            "mAP50"                to metrics.optDouble("metrics/mAP50(B)").format(),
            "mAP50-95"             to metrics.optDouble("metrics/mAP50-95(B)").format()
        )

        for ((key, value) in data) {
            val row = TableRow(context)
            row.addView(createTableCell(context, key))
            row.addView(createTableCell(context, value))
            tableLayout.addView(row)
        }

        AlertDialog.Builder(context)
            .setTitle("Training Metrics")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showConfusionMatrixDialog(context: Context, confusionMatrix: List<List<Float>>, classes: List<String>) {
        // Validate input
        if (confusionMatrix.size != classes.size) {
            throw IllegalArgumentException("The number of rows in the confusion matrix must match the number of classes.")
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confusion_matrix, null)
        val tableLayout: TableLayout = dialogView.findViewById(R.id.tableLayout)

        // Add table header
        val headerRow = TableRow(context).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
        headerRow.addView(createTableCell(context, " "))
        for (className in classes) {
            headerRow.addView(createTableCell(context, className))
        }
        tableLayout.addView(headerRow)

        // Add data rows
        for (i in confusionMatrix.indices) {
            val row = TableRow(context).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }

            // Add row header
            row.addView(createTableCell(context, classes[i]))

            // Add confusion matrix values
            for (j in confusionMatrix[i].indices) {
                row.addView(createTableCell(context, confusionMatrix[i][j].toString()))
            }

            tableLayout.addView(row)
        }

        // Show dialog
        AlertDialog.Builder(context)
            .setTitle("Confusion Matrix")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun Double.format(): String = if (this.isNaN()) "-" else "%.5f".format(this)
    private fun Double.formatPercent(): String = if (this.isNaN()) "-" else "${"%.2f".format(this * 100)}%"

    private fun createTableCell(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            val border = GradientDrawable()
            border.setColor(Color.WHITE) // Background putih
            border.setStroke(2, Color.BLACK) // Border hitam
            background = border
        }
    }
}