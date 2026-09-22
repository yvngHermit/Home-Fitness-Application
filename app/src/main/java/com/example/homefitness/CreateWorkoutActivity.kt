package com.example.homefitness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityCreateWorkoutBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class CreateWorkoutActivity : AppCompatActivity() {

    private lateinit var binding:ActivityCreateWorkoutBinding

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val selectedExercises = mutableListOf<Int>()
    private var exercises = emptyList<com.example.homefitness.models.WgerExercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityCreateWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveWorkoutButton.setOnClickListener {
            saveWorkout()
        }

        loadExercises()
    }

    private fun loadExercises() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitInstance.wgerApi.getExercises()

                if (response.isSuccessful && response.body() != null) {
                    exercises = response.body()!!.results
                } else {
                    Toast.makeText(
                        this@CreateWorkoutActivity,
                        "Could not load exercises",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateWorkoutActivity,
                    "Unable to load exercises",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveWorkout() {
        val name = binding.workoutNameInput.text.toString().trim()
        val description = binding.workoutDescriptionInput.text.toString().trim()

        if (name.isEmpty()) {
            binding.workoutNameInput.error = "Enter a workout name"
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        val workoutId = UUID.randomUUID().toString()

        val workout = com.example.homefitness.models.CustomWorkout(
            id = workoutId,
            name = name,
            description = description.ifEmpty { "Custom workout" },
            exercises = selectedExercises
        )

        db.collection("users")
            .document(user.uid)
            .collection("customWorkouts")
            .document(workoutId)
            .set(workout)
            .addOnSuccessListener {
                Toast.makeText(this, "Workout saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not save workout", Toast.LENGTH_SHORT).show()
            }
    }
}