package com.example.homefitness

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.homefitness.databinding.ActivityStartWorkoutBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StartWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartWorkoutBinding

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private var timer: CountDownTimer? = null

    private var timeRemaining = 60_000L
    private var isPaused = false

    private var exerciseId = -1
    private var exerciseName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityStartWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseId = intent.getIntExtra("exerciseId", -1)

        // Ensure a valid display name is used if intent extra was empty
        val rawName = intent.getStringExtra("exerciseName")
        exerciseName = if (!rawName.isNullOrBlank()) rawName else "Exercise #$exerciseId"

        binding.workoutTitle.text = exerciseName
        binding.currentExerciseName.text = exerciseName

        binding.exerciseProgressText.text = "Exercise 1"
        binding.workoutProgressBar.max = 60
        binding.workoutProgressBar.progress = 0

        // Handle Back button click
        binding.backButton.setOnClickListener {
            finish()
        }

        // Handle Pause / Resume toggle button
        binding.pauseButton.setOnClickListener {
            toggleTimer()
        }

        binding.nextButton.setOnClickListener {
            completeWorkout()
        }

        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        isPaused = false

        // Update ImageButton icon to Pause
        binding.pauseButton.setImageResource(android.R.drawable.ic_media_pause)

        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                updateTimerText()

                val elapsedSeconds = ((60_000L - millisUntilFinished) / 1000).toInt()
                binding.workoutProgressBar.progress = elapsedSeconds
            }

            override fun onFinish() {
                timeRemaining = 0
                updateTimerText()
                binding.workoutProgressBar.progress = 60
                completeWorkout()
            }
        }.start()
    }

    private fun toggleTimer() {
        if (isPaused) {
            startTimer()
        } else {
            timer?.cancel()
            isPaused = true
            // Update ImageButton icon to Play/Resume
            binding.pauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun updateTimerText() {
        val seconds = timeRemaining / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        binding.timerText.text = if (minutes > 0) {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
        } else {
            String.format(Locale.getDefault(), "%02d", remainingSeconds)
        }
    }

    private fun completeWorkout() {
        timer?.cancel()
        updateWorkoutStreak()

        Toast.makeText(
            this,
            "Workout completed!",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun updateWorkoutStreak() {
        val user = auth.currentUser ?: return
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            val currentStreak = document.getLong("streak")?.toInt() ?: 0
            val lastWorkout = document.getString("lastWorkoutDate") ?: ""
            val today = getTodayDate()

            if (lastWorkout == today) return@addOnSuccessListener

            val yesterday = getYesterdayDate()
            val newStreak = if (lastWorkout == yesterday) currentStreak + 1 else 1

            userRef.update(
                mapOf(
                    "streak" to newStreak,
                    "lastWorkoutDate" to today
                )
            )
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}