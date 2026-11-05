package com.example.homefitness.models

import com.google.gson.annotations.SerializedName

data class WgerExerciseResponse(
    val count: Int = 0,
    val results: List<WgerExercise> = emptyList()
)

data class WgerExercise(
    val id: Int?,
    @SerializedName("name") val rawName: String? = null,
    @SerializedName("description") val rawDescription: String? = null,
    val category: Any? = null,
    val equipment: List<WgerEquipment>? = null,
    val translations: List<WgerTranslation>? = null,
    val images: List<WgerExerciseImage>? = null
) {
    // exerciseinfo commonly supplies names in translations rather than at the top level.
    // Prefer English (wger language ID 2), but use another available translation as a fallback.
    val name: String?
        get() = rawName?.trim()?.takeIf { it.isNotEmpty() }
            ?: translations.orEmpty().firstOrNull { it.language == 2 && !it.name.isNullOrBlank() }?.name?.trim()
            ?: translations.orEmpty().firstOrNull { !it.name.isNullOrBlank() }?.name?.trim()

    val description: String?
        get() = rawDescription?.takeIf { it.isNotBlank() }
            ?: translations.orEmpty().firstOrNull { it.language == 2 && !it.description.isNullOrBlank() }?.description
            ?: translations.orEmpty().firstOrNull { !it.description.isNullOrBlank() }?.description
}

data class WgerTranslation(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val language: Int? = null
)

data class WgerEquipment(val id: Int? = null, val name: String? = null)

data class WgerExerciseImage(
    val image: String? = null,
    @SerializedName("is_main") val isMain: Boolean = false
) {
    fun preferredUrl(): String? = image
}
