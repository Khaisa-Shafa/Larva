package com.dicoding.skripsiapp.data

import com.google.gson.annotations.SerializedName

data class ClassificationReport(

	@SerializedName("Unknown")
	val unknown: Unknown,

	@SerializedName("weighted avg")
	val weightedAvg: WeightedAvg,

	@SerializedName("accuracy")
	val accuracy: Double,

	@SerializedName("Culex")
	val culex: Culex,

	@SerializedName("macro avg")
	val macroAvg: MacroAvg,

	@SerializedName("Aedes")
	val aedes: Aedes
)

data class MacroAvg(

	@SerializedName("f1-score")
	val f1Score: Double,

	@SerializedName("precision")
	val precision: Double,

	@SerializedName("recall")
	val recall: Double,

	@SerializedName("support")
	val support: Double
)

data class WeightedAvg(

	@SerializedName("f1-score")
	val f1Score: Double,

	@SerializedName("precision")
	val precision: Double,

	@SerializedName("recall")
	val recall: Double,

	@SerializedName("support")
	val support: Double
)

data class Unknown(

	@SerializedName("f1-score")
	override val f1Score: Double,

	@SerializedName("precision")
	override val precision: Double,

	@SerializedName("recall")
	override val recall: Double,

	@SerializedName("support")
	override val support: Double
) : Metrics


data class Aedes(

	@SerializedName("f1-score")
	override val f1Score: Double,

	@SerializedName("precision")
	override val precision: Double,

	@SerializedName("recall")
	override val recall: Double,

	@SerializedName("support")
	override val support: Double
) : Metrics


data class Culex(

	@SerializedName("f1-score")
	override val f1Score: Double,

	@SerializedName("precision")
	override val precision: Double,

	@SerializedName("recall")
	override val recall: Double,

	@SerializedName("support")
	override val support: Double
) : Metrics

