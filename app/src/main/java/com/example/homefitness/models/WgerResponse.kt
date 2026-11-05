package com.example.homefitness.models

import com.google.gson.annotations.SerializedName

data class WgerExerciseResponse(
    val count: Int,
    val results: List<WgerExercise>
)

data class WgerExercise(
    val id: Int?,
    @SerializedName("name")
    val rawName: String?,
    val description: String? = "",
    val category: Any? = null,
    val equipment: List<WgerEquipment>? = null,
    val translations: List<WgerTranslation>? = null
) {
    // Helper property: Gets raw name, or falls back to the first translation name
    val name: String?
        get() = when {
            !rawName.isNullOrBlank() -> rawName
            !translations.isNullOrEmpty() -> translations.firstOrNull { !it.name.isNullOrBlank() }?.name
            else -> null
        }
}

data class WgerTranslation(
    val id: Int?,
    val name: String?,
    val description: String?
)

data class WgerEquipment(val id: Int? = null, val name: String? = null)
