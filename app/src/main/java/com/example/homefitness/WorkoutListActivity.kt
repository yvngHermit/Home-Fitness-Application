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
import com.example.homefitness.data.ExerciseCache
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
        category = intent.getStringExtra("category")
            ?: intent.getStringExtra("categoryName")
                    ?: "Full Body"

        setupBottomNavigation()
        binding.workoutCategoryTitle.text = category
        binding.workoutCategoryDescription.text = "Choose a workout to begin"
        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExerciseAdapter { exercise ->
            startActivity(Intent(this, StartWorkoutActivity::class.java).apply {
                putExtra("exerciseId", exercise.id)
                putExtra("exerciseName", exercise.name)
                putExtra("exerciseDescription", exercise.description)
            })
        }
        binding.exerciseRecyclerView.adapter = adapter
        loadExercises()
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_workouts
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_workouts -> { startActivity(Intent(this, WorkoutCategoriesActivity::class.java)); finish(); true }
                R.id.nav_nutrition -> { startActivity(Intent(this, NutritionActivity::class.java)); finish(); true }
                R.id.nav_progress -> { startActivity(Intent(this, ProgressActivity::class.java)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }

    private fun loadExercises() {
        lifecycleScope.launch {
            binding.exerciseCountText.text = "Loading exercises..."
            try {
                // Do not send a single category ID here: broad app categories contain
                // several wger categories (e.g. upper body = arms/chest/back/shoulders).
                val response = RetrofitInstance.wgerApi.getExercises(
                    language = 2,
                    limit = 100,
                    offset = 0,
                    categoryId = null
                )
                if (response.isSuccessful) {
                    val all = response.body()?.results.orEmpty()
                    val cache = ExerciseCache(applicationContext)
                    cache.save("wger_all", all)
                    cache.save("wger_category_${category.lowercase()}", all)
                    cache.close()
                    val filtered = filterExercisesLocally(all)
                    adapter.submitList(filtered)
                    binding.exerciseCountText.text = "${filtered.size} exercises"
                    if (filtered.isEmpty()) {
                        binding.exerciseCountText.text = "No exercises available in this category"
                    }
                } else {
                    showCachedExercises("Server error ${response.code()}")
                }
            } catch (e: Exception) {
                showCachedExercises("Connection unavailable")
            }
        }
    }

    private fun showCachedExercises(message: String) {
        val cache = ExerciseCache(applicationContext)
        val saved = cache.read("wger_all").ifEmpty {
            cache.read("wger_category_${category.lowercase()}")
        }
        cache.close()
        val filtered = filterExercisesLocally(saved)
        adapter.submitList(filtered)
        binding.exerciseCountText.text = if (filtered.isEmpty()) message else "${filtered.size} saved exercises (offline)"
        if (filtered.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun filterExercisesLocally(exercises: List<WgerExercise>): List<WgerExercise> {
        val named = exercises.filter { it.id != null && !it.name.isNullOrBlank() }
            .distinctBy { it.id }
        return when (category.trim().lowercase()) {
            "upper body" -> named.filter { categoryId(it.category) in setOf(8, 11, 12, 13) }
            "lower body" -> named.filter { categoryId(it.category) in setOf(9, 14) }
            "core" -> named.filter { categoryId(it.category) == 10 }
            "cardio" -> named.filter { categoryId(it.category) == 15 }
            else -> named
        }
    }

    private fun categoryId(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        is Map<*, *> -> (value["id"] as? Number)?.toInt()
        else -> null
    }
}
