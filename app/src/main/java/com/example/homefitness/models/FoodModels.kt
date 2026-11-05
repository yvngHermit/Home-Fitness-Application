package com.example.homefitness.models

import com.google.gson.annotations.SerializedName

data class FoodSearchResponse(val foods: List<FoodItem> = emptyList())
data class FoodItem(
    val fdcId: Long = 0,
    val description: String = "",
    val brandName: String? = null,
    val foodNutrients: List<FoodNutrient> = emptyList()
) {
    fun calories(): Double = foodNutrients.firstOrNull { it.nutrientId == 1008 }?.value ?: 0.0
}
data class FoodNutrient(@SerializedName("nutrientId") val nutrientId: Int = 0, val value: Double = 0.0, val unitName: String? = null)
