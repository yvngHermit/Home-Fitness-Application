package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homefitness.adapters.CustomWorkoutAdapter
import com.example.homefitness.databinding.ActivityCustomWorkoutsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class CustomWorkoutsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomWorkoutsBinding

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: CustomWorkoutAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityCustomWorkoutsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Direct Bottom Navigation Setup
        setupBottomNavigation()

        binding.customWorkoutRecyclerView.layoutManager =
            LinearLayoutManager(this)

        adapter = CustomWorkoutAdapter { workout ->
            val intent = Intent(this, StartWorkoutActivity::class.java)
            intent.putExtra("exerciseId", -1)
            intent.putExtra("exerciseIds", workout.exercises.toIntArray())
            intent.putExtra("exerciseName", workout.name)
            startActivity(intent)
        }

        binding.customWorkoutRecyclerView.adapter = adapter

        binding.createWorkoutButton.setOnClickListener {
            startActivity(
                Intent(this, CreateWorkoutActivity::class.java)
            )
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        loadCustomWorkouts()
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
                R.id.nav_workouts -> true // Already on workouts section
                R.id.nav_nutrition -> { startActivity(Intent(this, NutritionActivity::class.java)); true }
                R.id.nav_progress -> { startActivity(Intent(this, ProgressActivity::class.java)); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun loadCustomWorkouts() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("customWorkouts")
            .get()
            .addOnSuccessListener { result ->
                val workouts = result.documents.mapNotNull {
                    it.toObject(com.example.homefitness.models.CustomWorkout::class.java)
                }
                adapter.submitList(workouts)
            }
    }
}