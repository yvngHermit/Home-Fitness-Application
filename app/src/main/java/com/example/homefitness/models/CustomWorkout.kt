package com.example.homefitness.models

data class CustomWorkout(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var exercises: List<Int> = emptyList()
)
