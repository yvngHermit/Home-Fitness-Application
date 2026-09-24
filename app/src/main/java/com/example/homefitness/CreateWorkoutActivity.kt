package com.example.homefitness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homefitness.adapters.SelectExerciseAdapter
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.databinding.ActivityCreateWorkoutBinding
import com.example.homefitness.models.CustomWorkout
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class CreateWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateWorkoutBinding

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val selectedExercises = mutableListOf<Int>()
    private lateinit var selectAdapter: SelectExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCreateWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)

        selectAdapter = SelectExerciseAdapter { exercise, isSelected ->
            exercise.id?.let { id ->
                if (isSelected) {
                    if (!selectedExercises.contains(id)) selectedExercises.add(id)
                } else {
                    selectedExercises.remove(id)
                }
            }
        }

        binding.exerciseRecyclerView.adapter = selectAdapter

        // Back button listener
        binding.backButton.setOnClickListener {
            finish()
        }

        // Save workout button listener
        binding.saveWorkoutButton.setOnClickListener {
            saveWorkout()
        }

        loadExercises()
    }

    private fun loadExercises() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d("API_TEST", "Sending request to Wger exerciseinfo API...")

                val response = RetrofitInstance.wgerApi.getExercises(limit = 50)

                android.util.Log.d("API_TEST", "Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val rawExercises = response.body()!!.results
                    android.util.Log.d("API_TEST", "Total items returned: ${rawExercises.size}")

                    // Filter out any items without a valid name (using our custom getter)
                    val validExercises = rawExercises.filter { !it.name.isNullOrBlank() }
                    android.util.Log.d("API_TEST", "Valid items count: ${validExercises.size}")

                    if (validExercises.isNotEmpty()) {
                        selectAdapter.submitList(validExercises)
                    } else {
                        Toast.makeText(
                            this@CreateWorkoutActivity,
                            "API returned 0 valid names",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@CreateWorkoutActivity,
                        "Server Error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("API_TEST", "Network Call Failed", e)
                Toast.makeText(
                    this@CreateWorkoutActivity,
                    "Network Exception: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
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

        if (selectedExercises.isEmpty()) {
            Toast.makeText(this, "Please select at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        val workoutId = UUID.randomUUID().toString()

        val workout = CustomWorkout(
            id = workoutId,
            name = name,
            description = description.ifEmpty { "Custom workout" },
            exerciseCount = selectedExercises.size,
            exercises = selectedExercises
        )

        db.collection("users")
            .document(user.uid)
            .collection("customWorkouts")
            .document(workoutId)
            .set(workout)
            .addOnSuccessListener {
                Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not save workout", Toast.LENGTH_SHORT).show()
            }
    }
}