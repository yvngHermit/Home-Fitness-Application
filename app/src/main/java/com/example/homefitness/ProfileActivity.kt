package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private val auth=FirebaseAuth.getInstance(); private val db=FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_profile);setupNav();findViewById<android.view.View>(R.id.settingsButton).setOnClickListener{startActivity(Intent(this,SettingsActivity::class.java))};findViewById<android.view.View>(R.id.editPreferencesButton).setOnClickListener{startActivity(Intent(this,OnboardingActivity::class.java).putExtra("editMode",true))};findViewById<android.view.View>(R.id.profileSignOutButton).setOnClickListener{auth.signOut();startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()};loadProfile()}
    private fun setupNav(){val nav=findViewById<BottomNavigationView>(R.id.bottomNavigation);nav?.selectedItemId=R.id.nav_profile;nav?.setOnItemSelectedListener{item->when(item.itemId){R.id.nav_home->{startActivity(Intent(this,HomeActivity::class.java));finish();true};R.id.nav_workouts->{startActivity(Intent(this,WorkoutCategoriesActivity::class.java));finish();true};R.id.nav_nutrition->{startActivity(Intent(this,NutritionActivity::class.java));finish();true};R.id.nav_progress->{startActivity(Intent(this,ProgressActivity::class.java));finish();true};R.id.nav_profile->true;else->false}}}
    private fun loadProfile() {
        val user = auth.currentUser ?: return
        findViewById<TextView>(R.id.profileName).text = user.displayName ?: user.email ?: "User"
        findViewById<TextView>(R.id.profileEmail).text = user.email ?: ""
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val goal = document.getString("fitnessGoal") ?: "Not set"
            val experience = document.getString("experienceLevel") ?: "Not set"
            val equipment = document.getString("equipment") ?: "Not set"
            val diet = document.getString("dietPreference") ?: "Not set"
            findViewById<TextView>(R.id.profilePreferences).text = "Goal: $goal\nExperience: $experience\nEquipment: $equipment\nDiet: $diet"
        }
    }
}
