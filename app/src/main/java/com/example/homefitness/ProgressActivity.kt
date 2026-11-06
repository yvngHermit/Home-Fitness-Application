package com.example.homefitness

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val green = 0xFF245B36.toInt()
    private val lime = 0xFFA8DF3D.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        setupNav()
        loadProgress()
    }

    private fun setupNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.apply {
            selectedItemId = R.id.nav_progress
            setOnItemSelectedListener { item ->
                val target = when (item.itemId) {
                    R.id.nav_home -> HomeActivity::class.java
                    R.id.nav_workouts -> WorkoutCategoriesActivity::class.java
                    R.id.nav_nutrition -> NutritionActivity::class.java
                    R.id.nav_profile -> ProfileActivity::class.java
                    else -> null
                }
                if (target != null) { startActivity(Intent(this@ProgressActivity, target)); finish(); true }
                else item.itemId == R.id.nav_progress
            }
        }
    }

    private fun loadProgress() {
        val uid = auth.currentUser?.uid ?: return
        val container = findViewById<LinearLayout>(R.id.progressContent)
        db.collection("users").document(uid).get().addOnSuccessListener { user ->
            val streak = user.getLong("streak") ?: 0L
            val weight = user.getDouble("weight")
            val unit = getSharedPreferences("home_fitness_settings", MODE_PRIVATE).getString("weight_unit", "kg") ?: "kg"
            addMetricCard(container, getString(R.string.current_streak_title), getString(R.string.days_value, streak), getString(R.string.consistency_subtitle))
            addMetricCard(container, "LATEST WEIGHT", weight?.let { String.format(Locale.getDefault(), "%.1f %s", if (unit == "lb") it * 2.20462 else it, unit) } ?: getString(R.string.not_recorded), getString(R.string.update_weight_subtitle))
        }
        db.collection("users").document(uid).collection("workoutLogs")
            .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get().addOnSuccessListener { docs ->
                container.removeAllViews()
                db.collection("users").document(uid).get().addOnSuccessListener { user ->
                    addMetricCard(container, getString(R.string.current_streak_title), getString(R.string.days_value, user.getLong("streak") ?: 0L), getString(R.string.consistency_subtitle))
                    val unit = getSharedPreferences("home_fitness_settings", MODE_PRIVATE).getString("weight_unit", "kg") ?: "kg"
                    addMetricCard(container, "LATEST WEIGHT", user.getDouble("weight")?.let { String.format(Locale.getDefault(), "%.1f %s", if (unit == "lb") it * 2.20462 else it, unit) } ?: getString(R.string.not_recorded), getString(R.string.update_weight_subtitle))
                }
                addMetricCard(container, getString(R.string.workouts_completed_title), docs.size().toString(), getString(R.string.recent_history_subtitle))
                addSectionTitle(container, getString(R.string.weekly_activity))
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val today = java.util.Calendar.getInstance()
                val counts = IntArray(7)
                docs.forEach { doc ->
                    val dateText = doc.getString("date") ?: return@forEach
                    try {
                        val parsed = dateFormat.parse(dateText) ?: return@forEach
                        val cal = java.util.Calendar.getInstance().apply { time = parsed }
                        val diff = ((today.timeInMillis - cal.timeInMillis) / 86400000L).toInt()
                        if (diff in 0..6) {
                            val index = ((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7)
                            counts[index]++
                        }
                    } catch (_: Exception) {}
                }
                val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
                val bars = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM; setPadding(4, 12, 4, 12) }
                dayNames.forEachIndexed { index, day ->
                    val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, 150, 1f) }
                    val bar = View(this).apply {
                        background = GradientDrawable().apply { cornerRadius = 18f; setColor(if (counts[index] > 0) lime else 0xFFE6EDE3.toInt()) }
                        layoutParams = LinearLayout.LayoutParams(22, (20 + 90 * counts[index] / max).coerceAtLeast(20))
                    }
                    col.addView(bar)
                    col.addView(TextView(this).apply { text = day; textSize = 11f; setTextColor(green); setPadding(0, 8, 0, 0) })
                    bars.addView(col)
                }
                container.addView(bars)
                addSectionTitle(container, getString(R.string.recent_workouts))
                if (docs.isEmpty) addMetricCard(container, getString(R.string.no_workouts_yet), getString(R.string.history_starts), getString(R.string.complete_to_see_progress))
                docs.forEach { doc ->
                    val name = doc.getString("workoutName") ?: getString(R.string.workouts)
                    val date = doc.getString("date") ?: ""
                    val count = doc.getLong("exerciseCount") ?: 1L
                    addMetricCard(container, name, "$date  •  $count ${getString(R.string.exercises).lowercase(Locale.getDefault())}", getString(R.string.completed_workout))
                }
            }.addOnFailureListener {
                addMetricCard(container, getString(R.string.progress_unavailable), getString(R.string.history_load_error), getString(R.string.check_connection))
            }
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply { text = title; textSize = 19f; setTypeface(null, Typeface.BOLD); setTextColor(green); setPadding(2, 18, 2, 8) })
    }

    private fun addMetricCard(parent: LinearLayout, title: String, value: String, subtitle: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            background = GradientDrawable().apply { setColor(0xFFFFFFFF.toInt()); cornerRadius = 28f; setStroke(1, 0xFFE4EBDD.toInt()) }
            elevation = 5f
        }
        card.addView(TextView(this).apply { text = title.uppercase(Locale.getDefault()); textSize = 11f; setTextColor(0xFF687568.toInt()); setTypeface(null, Typeface.BOLD) })
        card.addView(TextView(this).apply { text = value; textSize = 24f; setTextColor(green); setTypeface(null, Typeface.BOLD); setPadding(0, 6, 0, 2) })
        card.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(0xFF778077.toInt()) })
        val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 8, 0, 8); parent.addView(card, lp)
    }
}
