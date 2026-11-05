package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProgressActivity : AppCompatActivity() {
    private val db=FirebaseFirestore.getInstance(); private val auth=FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(R.layout.activity_progress); setupNav(); loadProgress() }
    private fun setupNav(){ val nav=findViewById<BottomNavigationView>(R.id.bottomNavigation); nav?.selectedItemId=R.id.nav_progress; nav?.setOnItemSelectedListener { item -> when(item.itemId){ R.id.nav_home->{startActivity(Intent(this,HomeActivity::class.java));finish();true};R.id.nav_workouts->{startActivity(Intent(this,WorkoutCategoriesActivity::class.java));finish();true};R.id.nav_nutrition->{startActivity(Intent(this,NutritionActivity::class.java));finish();true};R.id.nav_progress->true;R.id.nav_profile->{startActivity(Intent(this,ProfileActivity::class.java));finish();true};else->false } } }
    private fun loadProgress() {
        val uid = auth.currentUser?.uid ?: return
        val list = findViewById<android.widget.LinearLayout>(R.id.progressList)
        val summary = findViewById<TextView>(R.id.progressSummary)
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val streak = document.getLong("streak") ?: 0
            val weight = document.getDouble("weight")?.let { "$it kg" } ?: "Not recorded"
            summary.text = "Current streak: $streak days\nWeight: $weight"
        }
        db.collection("users").document(uid).collection("workoutLogs")
            .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get().addOnSuccessListener { docs ->
                list.removeAllViews()
                if (docs.isEmpty) {
                    val message = TextView(this)
                    message.text = "No completed workouts yet. Finish a workout to start tracking your progress."
                    list.addView(message)
                } else {
                    docs.forEach { document ->
                        val item = TextView(this)
                        val name = document.getString("workoutName") ?: "Workout"
                        val date = document.getString("date") ?: ""
                        val count = document.getLong("exerciseCount") ?: 1
                        item.text = "$name\n$date • $count exercises"
                        item.textSize = 16f
                        item.setPadding(8, 14, 8, 14)
                        list.addView(item)
                    }
                }
            }
    }
}
