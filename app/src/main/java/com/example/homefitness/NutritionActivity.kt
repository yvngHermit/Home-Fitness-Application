package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.api.FoodDataClient
import com.example.homefitness.databinding.ActivityNutritionBinding
import com.example.homefitness.models.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NutritionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNutritionBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.nutritionSettingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.foodSearchButton.setOnClickListener { searchFoods() }
        binding.suggestFoodsButton.setOnClickListener { suggestFoodsFromPreferences() }
        setupNavigation()
        loadTodayCalories()
        loadDietPreferences()
    }
    private fun setupNavigation() {
        val nav = binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        nav?.selectedItemId = R.id.nav_nutrition
        nav?.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); finish(); true }
                R.id.nav_workouts -> { startActivity(Intent(this, WorkoutCategoriesActivity::class.java)); finish(); true }
                R.id.nav_nutrition -> true
                R.id.nav_progress -> { startActivity(Intent(this, ProgressActivity::class.java)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }

    private var dietSearchTerm = "healthy high protein foods"
    private fun loadDietPreferences() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { profile ->
            val diet = profile.getString("dietPreference") ?: "none"
            val allergies = profile.getString("foodAllergies").orEmpty()
            dietSearchTerm = when (diet) {
                "vegetarian" -> "vegetarian high protein foods"
                "vegan" -> "plant based protein foods"
                "pescatarian" -> "fish and plant protein foods"
                "halal" -> "chicken and protein foods"
                "kosher" -> "fish and protein foods"
                else -> "healthy high protein foods"
            }
            val certificationNote = if (diet == "halal" || diet == "kosher") " USDA food records do not certify Halal or Kosher status; verify certification yourself." else ""
            val caution = if (allergies.isNotBlank()) " Check ingredients and allergen information for: $allergies. Food records may not list every allergen." else " Always check ingredient and allergen labels."
            binding.nutritionSuggestionText.text = "Suggestions based on your ${diet} preference.$caution$certificationNote"
        }
    }
    private fun suggestFoodsFromPreferences() {
        binding.foodSearchInput.setText(dietSearchTerm)
        searchFoods()
    }
    private fun searchFoods() {
        val query = binding.foodSearchInput.text.toString().trim()
        if (query.isBlank()) { binding.foodSearchInput.error = getString(R.string.search_food); return }
        val key = getSharedPreferences("home_fitness_settings", MODE_PRIVATE).getString("usda_api_key", "")
        if (key.isNullOrBlank()) {
            Toast.makeText(this, "Add your USDA API key in Settings first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java)); return
        }
        binding.foodResultsContainer.removeAllViews()
        addMessage("Searching…")
        lifecycleScope.launch {
            try {
                val response = FoodDataClient.api.searchFoods(query, key)
                binding.foodResultsContainer.removeAllViews()
                if (response.isSuccessful) {
                    val foods = response.body()?.foods.orEmpty()
                    if (foods.isEmpty()) addMessage("No foods found. Try another search.")
                    foods.forEach { addFoodCard(it) }
                } else addMessage("Food search failed (${response.code()}). Check your API key and try again.")
            } catch (e: Exception) {
                binding.foodResultsContainer.removeAllViews()
                addMessage("Could not reach FoodData Central. Check your internet connection.")
            }
        }
    }
    private fun addMessage(message: String) { val t=TextView(this); t.text=message; t.setPadding(12,16,12,16); binding.foodResultsContainer.addView(t) }
    private fun addFoodCard(food: FoodItem) {
        val card = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,12,14,12); setBackgroundColor(0xFFF2F5F1.toInt()) }
        val title = TextView(this).apply { text=food.description; textSize=16f; setTextColor(0xFF202020.toInt()) }
        val brand = food.brandName?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
        val calories = TextView(this).apply { text="${food.calories().toInt()} kcal per 100 g$brand"; setPadding(0,6,0,8) }
        val add = Button(this).apply { text=getString(R.string.add_to_diary); setOnClickListener { addFoodToDiary(food) } }
        card.addView(title); card.addView(calories); card.addView(add)
        val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,0,12); binding.foodResultsContainer.addView(card,lp)
    }
    private fun addFoodToDiary(food: FoodItem) {
        val uid=auth.currentUser?.uid ?: return
        val date=SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val calories=food.calories().toInt()
        val entry= hashMapOf("foodId" to food.fdcId, "name" to food.description, "caloriesPer100g" to calories, "servingGrams" to 100, "calories" to calories, "date" to date, "createdAt" to FieldValue.serverTimestamp())
        val userRef=db.collection("users").document(uid)
        val logRef=userRef.collection("mealLogs").document("${date}_${UUID.randomUUID()}")
        logRef.set(entry)
            .addOnSuccessListener { Toast.makeText(this,"Added ${food.description}",Toast.LENGTH_SHORT).show(); loadTodayCalories() }
            .addOnFailureListener { Toast.makeText(this,"Could not save food entry",Toast.LENGTH_SHORT).show() }
    }
    private fun loadTodayCalories() {
        val uid=auth.currentUser?.uid ?: return
        val date=SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        db.collection("users").document(uid).collection("mealLogs").whereEqualTo("date",date).get().addOnSuccessListener { docs ->
            val total=docs.documents.sumOf { it.getLong("calories") ?: 0L }
            binding.todayCaloriesText.text=getString(R.string.calories_logged_today,total.toInt())
        }
    }
    override fun onResume() { super.onResume(); if (::binding.isInitialized) loadTodayCalories() }
}
