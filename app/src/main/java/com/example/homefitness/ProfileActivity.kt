package com.example.homefitness

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupNav()
        findViewById<android.view.View>(R.id.settingsButton).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<android.view.View>(R.id.editPreferencesButton).setOnClickListener { startActivity(Intent(this, OnboardingActivity::class.java).putExtra("editMode", true)) }
        findViewById<android.view.View>(R.id.profileSignOutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
        loadProfile()
    }

    private fun setupNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.apply {
            selectedItemId = R.id.nav_profile
            setOnItemSelectedListener { item ->
                val target = when (item.itemId) {
                    R.id.nav_home -> HomeActivity::class.java
                    R.id.nav_workouts -> WorkoutCategoriesActivity::class.java
                    R.id.nav_nutrition -> NutritionActivity::class.java
                    R.id.nav_progress -> ProgressActivity::class.java
                    else -> null
                }
                if (target != null) { startActivity(Intent(this@ProfileActivity, target)); finish(); true }
                else item.itemId == R.id.nav_profile
            }
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        findViewById<TextView>(R.id.profileName).text = user.displayName ?: user.email?.substringBefore("@") ?: getString(R.string.user_label)
        findViewById<TextView>(R.id.profileEmail).text = user.email.orEmpty()
        findViewById<TextView>(R.id.profileInitial).text = (user.displayName ?: user.email ?: "U").firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val values = listOf(
                getString(R.string.fitness_goal_label) to (doc.getString("fitnessGoal") ?: getString(R.string.not_set)),
                getString(R.string.experience_label) to (doc.getString("experienceLevel") ?: getString(R.string.not_set)),
                getString(R.string.equipment_label) to (doc.getString("equipment") ?: getString(R.string.not_set)),
                getString(R.string.diet_label) to (doc.getString("dietPreference") ?: getString(R.string.not_set))
            )
            findViewById<TextView>(R.id.profilePreferences).text = values.joinToString("\n") { "${it.first}: ${it.second.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }}" }
            findViewById<TextView>(R.id.profileGoalChip).text = values[0].second.replace('_', ' ')
        }
    }
}
