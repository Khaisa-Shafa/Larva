package com.dicoding.skripsiapp.util

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.adapter.ClassificationReportAdapter
import com.dicoding.skripsiapp.data.ClassificationReport
import com.dicoding.skripsiapp.data.ModelResults
import com.dicoding.skripsiapp.viewmodel.PageClassificationViewModel

object DialogUtilsClassification {

    fun showModelResultsDialog(context: Context, results: ModelResults) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_model_results, null, false)
        val tableLayout: TableLayout = dialogView.findViewById(R.id.tableLayoutModelResults)

        val data = listOf(
            "Training Accuracy" to String.format("%.12f", results.trainingAccuracy),
            "Training Loss" to String.format("%.12f", results.trainingLoss),
            "Validation Accuracy" to String.format("%.12f", results.validationAccuracy),
            "Validation Loss" to String.format("%.12f", results.validationLoss),
            "Test Accuracy" to String.format("%.12f", results.testAccuracy),
            "Test Loss" to String.format("%.12f", results.testLoss)
        )

        data.forEach { (title, value) ->
            val row = TableRow(context)
            row.addView(createTextView(context, title, isHeader = true))
            row.addView(createTextView(context, value))
            tableLayout.addView(row)
        }

        AlertDialog.Builder(context)
            .setTitle("Model Results")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showConfusionMatrixDialog(context: Context, confusionMatrix: List<List<Int>>, classes: List<String>?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confusion_matrix, null, false)
        val tableLayout: TableLayout = dialogView.findViewById(R.id.tableLayout)

        val headerRow = TableRow(context)
        headerRow.addView(createTextView(context, "", isHeader = true))
        classes?.forEach { className ->
            headerRow.addView(createTextView(context, className, isHeader = true))
        }
        tableLayout.addView(headerRow)

        confusionMatrix.forEachIndexed { rowIndex, row ->
            val tableRow = TableRow(context)
            tableRow.addView(createTextView(context, classes?.getOrNull(rowIndex) ?: "Unknown", isHeader = true))
            row.forEach { value ->
                tableRow.addView(createTextView(context, value.toString()))
            }
            tableLayout.addView(tableRow)
        }

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showClassificationReportDialog(context: Context, report: ClassificationReport) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_classification_report, null, false)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ClassificationReportAdapter(report)

        AlertDialog.Builder(context)
            .setTitle("Classification Report")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showFunFactDialog(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        className: String,
        viewModel: PageClassificationViewModel
    ) {
        // Tampilkan progress dialog selama data dimuat
        val progressDialog = AlertDialog.Builder(context)
            .setTitle("Fetching Fun Fact")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Menghapus observer sebelumnya sebelum menambahkan yang baru
        viewModel.funFact.removeObservers(lifecycleOwner)

        // Memanggil method ViewModel untuk mengambil fun fact
        viewModel.fetchFunFact(className)

        // Mengamati perubahan pada LiveData funFact
        viewModel.funFact.observe(lifecycleOwner, Observer { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Progress dialog sudah ditampilkan sebelumnya
                }
                is Resource.Success -> {
                    progressDialog.dismiss()

                    // Batasi fun fact hingga 200 kata
                    val truncatedFunFact = resource.data?.split(" ")?.take(200)?.joinToString(" ")

                    // Tampilkan fun fact dalam dialog
                    AlertDialog.Builder(context)
                        .setTitle("Fun Fact")
                        .setMessage(truncatedFunFact)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                is Resource.Error -> {
                    progressDialog.dismiss()

                    // Tampilkan pesan kesalahan jika gagal mendapatkan fun fact
                    AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage("Failed to fetch fun fact: ${resource.message}")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                else -> Unit
            }
        })
    }


    private fun createTextView(context: Context, text: String, isHeader: Boolean = false): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = if (isHeader) 16f else 14f
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER
            if (isHeader) setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.table_cell_border)
        }
    }
}