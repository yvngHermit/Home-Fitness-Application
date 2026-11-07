package com.example.homefitness

import com.example.homefitness.models.FoodItem
import com.example.homefitness.models.FoodNutrient
import com.example.homefitness.models.WgerExercise
import com.example.homefitness.models.WgerTranslation
import com.example.homefitness.models.CustomWorkout
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    // FOOD MODEL TESTS

    @Test
    fun foodItem_returnsCorrectCalories() {
        val food = FoodItem(
            fdcId = 123L,
            description = "Banana",
            foodNutrients = listOf(
                FoodNutrient(nutrientId = 1008, value = 105.0)
            )
        )

        assertEquals(105.0, food.calories(), 0.01)
    }

    @Test
    fun foodItem_returnsZeroWhenCaloriesAreMissing() {
        val food = FoodItem(
            fdcId = 124L,
            description = "Food without calorie data",
            foodNutrients = emptyList()
        )

        assertEquals(0.0, food.calories(), 0.01)
    }

    @Test
    fun foodItem_usesEnergyNutrientOnly() {
        val food = FoodItem(
            fdcId = 125L,
            description = "Test food",
            foodNutrients = listOf(
                FoodNutrient(nutrientId = 1003, value = 5.0),
                FoodNutrient(nutrientId = 1008, value = 200.0)
            )
        )

        assertEquals(200.0, food.calories(), 0.01)
    }

    // WGER EXERCISE MODEL TESTS

    @Test
    fun exercise_usesEnglishTranslation() {
        val exercise = WgerExercise(
            id = 1,
            rawName = null,
            translations = listOf(
                WgerTranslation(
                    id = 1,
                    name = "Push-ups",
                    language = 2
                )
            )
        )

        assertEquals("Push-ups", exercise.name)
    }

    @Test
    fun exercise_prefersEnglishTranslation() {
        val exercise = WgerExercise(
            id = 2,
            rawName = null,
            translations = listOf(
                WgerTranslation(
                    id = 1,
                    name = "Pompes",
                    language = 12
                ),
                WgerTranslation(
                    id = 2,
                    name = "Push-ups",
                    language = 2
                )
            )
        )

        assertEquals("Push-ups", exercise.name)
    }

    @Test
    fun exercise_usesTopLevelNameWhenAvailable() {
        val exercise = WgerExercise(
            id = 3,
            rawName = "Squats",
            translations = emptyList()
        )

        assertEquals("Squats", exercise.name)
    }

    @Test
    fun exercise_returnsNullWhenNoNameExists() {
        val exercise = WgerExercise(
            id = 4,
            rawName = null,
            translations = emptyList()
        )

        assertNull(exercise.name)
    }

    // CUSTOM WORKOUT TESTS

    @Test
    fun customWorkout_storesExerciseInformation() {
        val workout = CustomWorkout(
            id = "workout1",
            name = "Morning Workout",
            description = "Daily fitness routine",
            exerciseCount = 3,
            exercises = listOf(1, 2, 3)
        )

        assertEquals("Morning Workout", workout.name)
        assertEquals(3, workout.exerciseCount)
        assertEquals(3, workout.exercises.size)
    }
}
