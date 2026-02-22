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

        // Data to be displayed in the table
        val data = mapOf(
            "Epoch" to metrics.getInt("epoch").toString(),
            "Train Box Loss" to metrics.getDouble("train/box_loss").toString(),
            "Train Cls Loss" to metrics.getDouble("train/cls_loss").toString(),
            "Train Dfl Loss" to metrics.getDouble("train/dfl_loss").toString(),
            "Validation Box Loss" to metrics.getDouble("val/box_loss").toString(),
            "Validation Cls Loss" to metrics.getDouble("val/cls_loss").toString(),
            "Validation Dfl Loss" to metrics.getDouble("val/dfl_loss").toString(),
            "Precision" to metrics.getDouble("metrics/precision(B)").toString(),
            "Recall" to metrics.getDouble("metrics/recall(B)").toString(),
            "mAP50" to metrics.getDouble("metrics/mAP50(B)").toString(),
            "mAP50-95" to metrics.getDouble("metrics/mAP50-95(B)").toString()
        )

        // Populate the table with data and add borders
        for ((key, value) in data) {
            val row = TableRow(context)
            row.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // Key cell
            val keyView = createTableCell(context, key)
            // Value cell
            val valueView = createTableCell(context, value)

            // Add the cells to the row
            row.addView(keyView)
            row.addView(valueView)

            // Add the row to the table
            tableLayout.addView(row)
        }

        // Show dialog
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

    fun showFunFactDialog(
        context: Context,
        funFact: String,
        onDismiss: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Fun Fact")
        builder.setMessage(funFact)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        builder.setOnDismissListener {
            onDismiss?.invoke()
        }

        builder.create().show()
    }


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