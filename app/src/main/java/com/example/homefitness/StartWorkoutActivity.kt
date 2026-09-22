package com.example.homefitness

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
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
        exerciseName = intent.getStringExtra("exerciseName") ?: "Workout"

        binding.workoutTitle.text = exerciseName
        binding.currentExerciseName.text = exerciseName

        binding.exerciseProgressText.text = "Exercise 1"
        binding.workoutProgressBar.max = 100
        binding.workoutProgressBar.progress = 0

        binding.repsText.text = "10 reps"
        binding.weightText.text = "Bodyweight"

        // Safely set exercise information if the view is a TextView or contains one
        val infoDescription = intent.getStringExtra("exerciseDescription") ?: "Complete this exercise."
        val infoView = binding.root.findViewById<TextView>(R.id.exerciseInformation)
        infoView?.text = infoDescription

        // Setup pause button safely regardless of layout container type
        val pauseBtn = binding.root.findViewById<Button>(R.id.pauseButton)
            ?: binding.root.findViewById<TextView>(R.id.pauseButton)

        binding.pauseButton.setOnClickListener {
            toggleTimer(pauseBtn)
        }

        binding.nextButton.setOnClickListener {
            completeWorkout()
        }

        startTimer(pauseBtn)
    }

    private fun startTimer(pauseBtn: Any?) {
        timer?.cancel()
        isPaused = false

        updateButtonText(pauseBtn, "Pause")

        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                updateTimerText()

                val progress = (60_000L - millisUntilFinished).toInt()
                binding.workoutProgressBar.progress = progress * 100 / 60_000
            }

            override fun onFinish() {
                timeRemaining = 0
                updateTimerText()
                binding.workoutProgressBar.progress = 100
                completeWorkout()
            }
        }.start()
    }

    private fun toggleTimer(pauseBtn: Any?) {
        if (isPaused) {
            startTimer(pauseBtn)
        } else {
            timer?.cancel()
            isPaused = true
            updateButtonText(pauseBtn, "Resume")
        }
    }

    private fun updateButtonText(btnView: Any?, text: String) {
        when (btnView) {
            is Button -> btnView.text = text
            is TextView -> btnView.text = text
        }
    }

    private fun updateTimerText() {
        val seconds = timeRemaining / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        binding.timerText.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            remainingSeconds
        )
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