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

        // Direct Bottom Navigation Setup
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
                val response = RetrofitInstance.wgerApi.getExercises()

                if (response.isSuccessful && response.body() != null) {
                    val allExercises = response.body()!!.results
                    val exercises = filterExercises(allExercises)

                    adapter.submitList(exercises)
                    binding.exerciseCountText.text = "${exercises.size} exercises"
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
                    "Unable to connect to Wger",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun filterExercises(
        exercises: List<com.example.homefitness.models.WgerExercise>
    ): List<com.example.homefitness.models.WgerExercise> {

        return when (category) {
            "Full Body" -> exercises
            "Upper Body" -> exercises.filter { it.category in listOf(8, 11, 12, 13) }
            "Lower Body" -> exercises.filter { it.category in listOf(9, 14) }
            "Core" -> exercises.filter { it.category == 10 }
            "Cardio" -> exercises.filter { it.category == 15 }
            else -> exercises
        }
    }
}