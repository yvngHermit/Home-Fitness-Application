package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityHomepageBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private var todayWorkoutId = -1
    private var todayWorkoutName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bottom Navigation Setup
        setupBottomNavigation()

        loadUser()

        // Start Today's Workout Button (Matches default XML structure if ID isn't set, otherwise add android:id)
        binding.root.findViewById<android.widget.Button>(R.id.loginButton)?.setOnClickListener {
            launchWorkout()
        } ?: run {
            // Fallback click action for layout button container
            loadUser()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            loadUser()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNavigation.root as? BottomNavigationView
        bottomNav?.selectedItemId = R.id.nav_home

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_workouts -> {
                    startActivity(Intent(this, WorkoutCategoriesActivity::class.java))
                    true
                }
                else -> true
            }
        }
    }

    private fun launchWorkout() {
        val intent = Intent(this, StartWorkoutActivity::class.java)
        intent.putExtra("exerciseId", todayWorkoutId)
        intent.putExtra("exerciseName", if (todayWorkoutName.isNotEmpty()) todayWorkoutName else "Upper Body Strength")
        startActivity(intent)
    }

    private fun loadUser() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                val name = document.getString("name")
                    ?: user.displayName
                    ?: "User"

                val calorieGoal = document.getLong("calorieGoal")?.toInt() ?: 2000
                val calories = document.getLong("caloriesConsumed")?.toInt() ?: 1320
                val meals = document.getLong("mealsCalories")?.toInt() ?: 1180
                val streak = document.getLong("streak")?.toInt() ?: 7
                val weight = document.getDouble("weight") ?: 72.4
                val weightChange = document.getDouble("weightChange") ?: -0.6

                loadTodayWorkout(document)
            }
            .addOnFailureListener {
                loadRandomWorkout()
            }
    }

    private fun loadTodayWorkout(document: com.google.firebase.firestore.DocumentSnapshot) {
        val savedDate = document.getString("todayWorkoutDate")
        val today = getTodayDate()

        if (savedDate == today && document.contains("todayWorkoutId")) {
            todayWorkoutId = document.getLong("todayWorkoutId")?.toInt() ?: -1
            todayWorkoutName = document.getString("todayWorkoutName") ?: "Upper Body Strength"
        } else {
            loadRandomWorkout()
        }
    }

    private fun loadRandomWorkout() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitInstance.wgerApi.getExercises()
                if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                    val exercise = response.body()!!.results.random()
                    todayWorkoutId = exercise.id
                    todayWorkoutName = exercise.name
                    saveTodayWorkout(exercise.id, exercise.name)
                } else {
                    useDefaultWorkout()
                }
            } catch (e: Exception) {
                useDefaultWorkout()
            }
        }
    }

    private fun useDefaultWorkout() {
        todayWorkoutId = -1
        todayWorkoutName = "Upper Body Strength"
    }

    private fun saveTodayWorkout(id: Int, name: String) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(
            "todayWorkoutId" to id,
            "todayWorkoutName" to name,
            "todayWorkoutDate" to getTodayDate()
        )
        db.collection("users").document(user.uid).update(data as Map<String, Any>)
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}