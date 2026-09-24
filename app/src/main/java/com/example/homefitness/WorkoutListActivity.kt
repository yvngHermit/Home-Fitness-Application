package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homefitness.adapters.ExerciseAdapter
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityWorkoutListBinding
import com.example.homefitness.models.WgerExercise
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class WorkoutListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutListBinding
    private lateinit var adapter: ExerciseAdapter

    private var category = "Full Body"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityWorkoutListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = intent.getStringExtra("category") ?: "Full Body"

        setupBottomNavigation()

        binding.workoutCategoryTitle.text = category
        binding.workoutCategoryDescription.text = "Choose a workout to begin"

        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ExerciseAdapter { exercise ->
            val intent = Intent(this, StartWorkoutActivity::class.java)
            intent.putExtra("exerciseId", exercise.id)
            intent.putExtra("exerciseName", exercise.name)
            intent.putExtra("exerciseDescription", exercise.description)
            startActivity(intent)
        }

        binding.exerciseRecyclerView.adapter = adapter

        loadExercises()
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_workouts

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_workouts -> {
                    startActivity(Intent(this, WorkoutCategoriesActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }

    private fun loadExercises() {
        lifecycleScope.launch {
            try {
                val categoryId = getPrimaryCategoryId(category)

                // Fetch exercises with a limit of 100
                val response = RetrofitInstance.wgerApi.getExercises(
                    language = 2,
                    limit = 100,
                    categoryId = categoryId
                )

                if (response.isSuccessful && response.body() != null) {
                    var rawExercises = response.body()!!.results

                    // Filter valid named exercises
                    var validExercises = rawExercises.filter { !it.name.isNullOrBlank() }

                    // If API category filter returned no results, fetch general exercises as fallback
                    if (validExercises.isEmpty() && categoryId != null) {
                        val fallbackResponse = RetrofitInstance.wgerApi.getExercises(
                            language = 2,
                            limit = 100,
                            categoryId = null
                        )
                        if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                            val rawFallback = fallbackResponse.body()!!.results.filter { !it.name.isNullOrBlank() }
                            validExercises = filterExercisesLocally(rawFallback)
                        }
                    }

                    adapter.submitList(validExercises)
                    binding.exerciseCountText.text = "${validExercises.size} exercises"
                } else {
                    Toast.makeText(
                        this@WorkoutListActivity,
                        "Could not load exercises",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WorkoutListActivity,
                    "Unable to connect to Wger: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getPrimaryCategoryId(categoryName: String): Int? {
        return when (categoryName) {
            "Upper Body" -> 11 // Chest
            "Lower Body" -> 9  // Legs
            "Core"       -> 10 // Abs
            "Cardio"     -> 10 // Fallback ID for Cardio search
            else         -> null // "Full Body" or unspecified -> fetch overall list
        }
    }

    private fun filterExercisesLocally(exercises: List<WgerExercise>): List<WgerExercise> {
        return when (category) {
            "Full Body"  -> exercises
            "Upper Body" -> exercises.filter { extractCategoryId(it.category) in listOf(8, 11, 12, 13) }
            "Lower Body" -> exercises.filter { extractCategoryId(it.category) in listOf(9, 14) }
            "Core"       -> exercises.filter { extractCategoryId(it.category) == 10 }
            "Cardio"     -> exercises.filter { extractCategoryId(it.category) in listOf(8, 9, 10) }
            else         -> exercises
        }
    }

    // Helper to extract category ID whether it's returned as an Int or Map/Category Object
    private fun extractCategoryId(category: Any?): Int? {
        return when (category) {
            is Number -> category.toInt()
            is Map<*, *> -> (category["id"] as? Number)?.toInt()
            else -> null
        }
    }
}