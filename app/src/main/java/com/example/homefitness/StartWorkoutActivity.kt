package com.example.homefitness

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.homefitness.api.RetrofitInstance
import com.example.homefitness.data.ExerciseCache
import com.example.homefitness.databinding.ActivityStartWorkoutBinding
import com.example.homefitness.models.WgerExercise
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class StartWorkoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartWorkoutBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var exerciseIds = mutableListOf<Int>()
    private var exerciseNames = mutableListOf<String>()
    private var exerciseImages = mutableListOf<String?>()
    private var currentIndex = 0
    private var workoutName = "Workout"
    private var experienceLevel = "beginner"
    private var timer: CountDownTimer? = null
    private var remainingMillis = 0L
    private var timerRunning = false
    private var timedExercise = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workoutName = intent.getStringExtra("exerciseName") ?: "Workout"
        val ids = intent.getIntArrayExtra("exerciseIds")

        if (ids != null && ids.isNotEmpty()) {
            exerciseIds = ids.toMutableList()
        } else {
            val id = intent.getIntExtra("exerciseId", -1)
            if (id >= 0) exerciseIds.add(id)
        }

        binding.workoutTitle.text = workoutName
        binding.backButton.setOnClickListener { finish() }

        binding.exerciseInformation.visibility = View.GONE
        binding.pauseButton.setOnClickListener { toggleTimer() }
        binding.nextButton.setOnClickListener {
            if (timedExercise && remainingMillis > 0L) {
                Toast.makeText(this, "Complete the timer before continuing", Toast.LENGTH_SHORT).show()
            } else finishCurrentExercise()
        }

        loadExperienceLevel()

        if (exerciseIds.isEmpty()) {
            exerciseNames.add(workoutName)
            exerciseImages.add(null)
            showCurrentExercise()
        } else {
            loadExerciseDetails()
        }
    }

    private fun loadExperienceLevel() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { profile ->
                experienceLevel = profile.getString("experienceLevel") ?: "beginner"
                if (exerciseNames.isNotEmpty()) showCurrentExercise()
            }
    }

    private fun loadExerciseDetails() {
        lifecycleScope.launch {
            val cache = ExerciseCache(applicationContext)
            val cached = cache.read("wger_all")
            cache.close()

            exerciseNames.clear()
            exerciseImages.clear()

            for (id in exerciseIds) {
                val cachedExercise = cached.firstOrNull { it.id == id }
                val exercise = try {
                    val response = RetrofitInstance.wgerApi.getExerciseById(id)
                    if (response.isSuccessful) response.body() else null
                } catch (_: Exception) {
                    null
                }

                exerciseNames.add(
                    exercise?.name?.takeIf { it.isNotBlank() }
                        ?: cachedExercise?.name
                        ?: "Exercise #$id"
                )

                exerciseImages.add(
                    exercise?.let { preferredImage(it) }
                        ?: cachedExercise?.let { preferredImage(it) }
                )
            }

            showCurrentExercise()
        }
    }

    private fun preferredImage(exercise: WgerExercise): String? {
        return exercise.images.orEmpty().firstOrNull { it.isMain }?.preferredUrl()
            ?: exercise.images.orEmpty().firstOrNull()?.preferredUrl()
    }

    private fun isTimedExercise(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return listOf("plank", "wall sit", "hollow hold", "static hold", "isometric", "dead hang", "bridge hold", "superman hold", "hold", "front lever", "l-sit", "side bridge")
            .any { lower.contains(it) }
    }

    private fun durationForExercise(): Int = when (experienceLevel.lowercase(Locale.ROOT)) {
        "advanced" -> 45
        "intermediate" -> 30
        else -> 20
    }

    private fun targetForExercise(name: String): String {
        val n = name.lowercase(Locale.ROOT)
        if (isTimedExercise(name)) return "${durationForExercise()} seconds"
        val reps = when {
            listOf("burpee", "jump squat", "mountain climber", "high knees").any { n.contains(it) } -> if (experienceLevel == "beginner") "6–8" else "10–15"
            listOf("push up", "push-up", "pull up", "pull-up", "dip").any { n.contains(it) } -> if (experienceLevel == "beginner") "5–8" else "8–12"
            listOf("squat", "lunge", "step up", "step-up").any { n.contains(it) } -> if (experienceLevel == "beginner") "8–10" else "10–15"
            listOf("crunch", "sit up", "sit-up", "leg raise").any { n.contains(it) } -> if (experienceLevel == "beginner") "8–12" else "12–20"
            else -> if (experienceLevel == "beginner") "8–10" else "10–12"
        }
        val sets = when (experienceLevel.lowercase(Locale.ROOT)) {
            "advanced" -> 4
            "intermediate" -> 3
            else -> 2
        }
        return "$sets sets × $reps reps"
    }

    private fun toggleTimer() {
        if (!timedExercise || remainingMillis <= 0L) return
        if (timerRunning) {
            timer?.cancel()
            timerRunning = false
            binding.pauseButton.setImageResource(android.R.drawable.ic_media_play)
        } else startCountdown()
    }

    private fun startCountdown() {
        timer?.cancel()
        timerRunning = true
        binding.pauseButton.setImageResource(android.R.drawable.ic_media_pause)
        binding.nextButton.isEnabled = false
        timer = object : CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                binding.timerText.text = String.format(Locale.US, "%02d:%02d", millisUntilFinished / 60000, (millisUntilFinished / 1000) % 60)
            }
            override fun onFinish() {
                remainingMillis = 0L
                timerRunning = false
                binding.timerText.text = "00:00"
                binding.timerLabel.text = "TIME COMPLETE"
                binding.pauseButton.visibility = View.GONE
                binding.nextButton.isEnabled = true
                binding.nextButton.text = if (currentIndex == exerciseNames.lastIndex) "Finish workout" else "Complete exercise"
                Toast.makeText(this@StartWorkoutActivity, "Great work! Confirm to continue.", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun showCurrentExercise() {
        if (currentIndex >= exerciseNames.size) {
            completeWorkout()
            return
        }

        val name = exerciseNames[currentIndex]
        binding.currentExerciseName.text = name
        binding.exerciseProgressText.text = "Exercise ${currentIndex + 1} of ${exerciseNames.size}"
        binding.workoutProgressBar.max = exerciseNames.size.coerceAtLeast(1)
        binding.workoutProgressBar.progress = currentIndex + 1

        timer?.cancel()
        timerRunning = false
        timedExercise = isTimedExercise(name)
        binding.nextButton.isEnabled = true
        binding.nextButton.text = if (currentIndex == exerciseNames.lastIndex) "Finish workout" else "Complete exercise"
        if (timedExercise) {
            remainingMillis = durationForExercise() * 1000L
            binding.timerText.text = String.format(Locale.US, "00:%02d", durationForExercise())
            binding.timerText.textSize = 30f
            binding.timerLabel.text = "PRESS PLAY TO START"
            binding.pauseButton.visibility = View.VISIBLE
            binding.pauseButton.setImageResource(android.R.drawable.ic_media_play)
            binding.nextButton.isEnabled = false
        } else {
            remainingMillis = 0L
            binding.timerText.text = targetForExercise(name)
            binding.timerText.textSize = 18f
            binding.timerLabel.text = "SUGGESTED TARGET · CONFIRM WHEN DONE"
            binding.pauseButton.visibility = View.GONE
        }

        Glide.with(this)
            .load(exerciseImages.getOrNull(currentIndex))
            .placeholder(R.drawable.ic_workout)
            .error(R.drawable.ic_workout)
            .fitCenter()
            .into(binding.currentExerciseImage)
    }

    private fun finishCurrentExercise() {
        timer?.cancel()
        currentIndex++
        showCurrentExercise()
    }

    private fun completeWorkout() {
        timer?.cancel()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val userRef = db.collection("users").document(uid)

            userRef.collection("workoutLogs").document(UUID.randomUUID().toString()).set(
                mapOf(
                    "workoutName" to workoutName,
                    "exerciseCount" to exerciseNames.size,
                    "date" to today,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            )

            userRef.get().addOnSuccessListener { document ->
                val lastWorkoutDate = document.getString("lastWorkoutDate") ?: ""
                val currentStreak = document.getLong("streak")?.toInt() ?: 0

                if (lastWorkoutDate != today) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

                    userRef.set(
                        mapOf(
                            "streak" to if (lastWorkoutDate == yesterday) currentStreak + 1 else 1,
                            "lastWorkoutDate" to today
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            }
        }

        Toast.makeText(this, "Workout completed!", Toast.LENGTH_SHORT).show()
        finish()
    }
    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
