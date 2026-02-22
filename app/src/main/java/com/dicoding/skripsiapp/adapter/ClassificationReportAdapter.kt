package com.dicoding.skripsiapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.ClassificationReport

class ClassificationReportAdapter(private val report: ClassificationReport) :
    RecyclerView.Adapter<ClassificationReportAdapter.ViewHolder>() {

    // Data yang akan ditampilkan dalam tabel
    private val metrics = listOf(
        "Class" to listOf("Precision", "Recall", "F1-Score", "Support"),
        "Aedes" to listOf(report.aedes.precision, report.aedes.recall, report.aedes.f1Score, report.aedes.support),
        "Culex" to listOf(report.culex.precision, report.culex.recall, report.culex.f1Score, report.culex.support),
        "Unknown" to listOf(report.unknown.precision, report.unknown.recall, report.unknown.f1Score, report.unknown.support),
        "Macro Avg" to listOf(report.macroAvg.precision, report.macroAvg.recall, report.macroAvg.f1Score, report.macroAvg.support),
        "Weighted Avg" to listOf(report.weightedAvg.precision, report.weightedAvg.recall, report.weightedAvg.f1Score, report.weightedAvg.support),
        "Accuracy" to listOf(report.accuracy, "-", "-", "-")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_classification_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (label, values) = metrics[position]
        holder.bind(label, values)
    }

    override fun getItemCount(): Int = metrics.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvPrecision: TextView = itemView.findViewById(R.id.tvPrecision)
        private val tvRecall: TextView = itemView.findViewById(R.id.tvRecall)
        private val tvF1Score: TextView = itemView.findViewById(R.id.tvF1Score)
        private val tvSupport: TextView = itemView.findViewById(R.id.tvSupport)

        fun bind(label: String, values: List<Any>) {
            tvLabel.text = label

            // Format angka menjadi 2 digit desimal
            tvPrecision.text = if (values[0] is Number) String.format("%.2f", values[0]) else values[0].toString()
            tvRecall.text = if (values[1] is Number) String.format("%.2f", values[1]) else values[1].toString()
            tvF1Score.text = if (values[2] is Number) String.format("%.2f", values[2]) else values[2].toString()
            tvSupport.text = if (values[3] is Number) String.format("%.0f", values[3]) else values[3].toString() // Support biasanya bilangan bulat
        }


    }
}