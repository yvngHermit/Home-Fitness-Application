package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.homefitness.databinding.ActivityWorkoutCategoriesBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class WorkoutCategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutCategoriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityWorkoutCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation directly
        setupBottomNavigation()

        // Navigate to Custom Workout Page
        val customWorkoutBtn = binding.root.findViewById<Button>(R.id.customWorkoutButton)
        customWorkoutBtn?.setOnClickListener {
            startActivity(Intent(this, CustomWorkoutsActivity::class.java))
        }

        // Set up click listeners for workout categories
        binding.fullBodyCard?.setOnClickListener {
            openWorkoutList("Full Body")
        }

        binding.upperBodyCard?.setOnClickListener {
            openWorkoutList("Upper Body")
        }

        binding.lowerBodyCard?.setOnClickListener {
            openWorkoutList("Lower Body")
        }

        binding.coreCard?.setOnClickListener {
            openWorkoutList("Core")
        }

        binding.cardioCard?.setOnClickListener {
            openWorkoutList("Cardio")
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_workouts

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_workouts -> true // Already on categories screen
                R.id.nav_nutrition -> { startActivity(Intent(this, NutritionActivity::class.java)); true }
                R.id.nav_progress -> { startActivity(Intent(this, ProgressActivity::class.java)); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun openWorkoutList(category: String) {
        val intent = Intent(this, WorkoutListActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }
}