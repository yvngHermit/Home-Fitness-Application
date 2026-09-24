package com.example.homefitness.models

data class CustomWorkout(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val exerciseCount: Int = 0,
    val exercises: List<Int> = emptyList()
)