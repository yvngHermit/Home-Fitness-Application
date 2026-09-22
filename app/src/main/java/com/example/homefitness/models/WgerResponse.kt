package com.example.homefitness.models

data class WgerExerciseResponse(
    val count: Int,
    val results: List<WgerExercise>
)

data class WgerExercise(
    val id: Int,
    val name: String,
    val description: String,
    val category: Int
)
