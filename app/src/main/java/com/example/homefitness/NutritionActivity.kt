package com.example.homefitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homefitness.api.FoodDataClient
import com.example.homefitness.databinding.ActivityNutritionBinding
import com.example.homefitness.models.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NutritionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNutritionBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var dietSearchTerm = "healthy high protein foods"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nutritionSettingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.foodSearchButton.setOnClickListener { searchFoods() }
        binding.suggestFoodsButton.setOnClickListener { suggestFoodsFromPreferences() }

        setupNavigation()
        loadTodayCalories()
        loadDietPreferences()
    }

    private fun setupNavigation() {
        val nav = binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNavigation
        )
        nav?.selectedItemId = R.id.nav_nutrition
        nav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_workouts -> {
                    startActivity(Intent(this, WorkoutCategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_nutrition -> true
                R.id.nav_progress -> {
                    startActivity(Intent(this, ProgressActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

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

            val certificationNote = if (diet == "halal" || diet == "kosher") {
                " USDA records do not certify Halal or Kosher status; verify product certification yourself."
            } else ""
            val allergyNote = if (allergies.isNotBlank()) {
                " Check ingredient and allergen labels carefully for: $allergies. USDA records may not list every allergen."
            } else {
                " Always check ingredient and allergen labels."
            }

            binding.nutritionSuggestionText.text =
                "Suggestions based on your $diet preference.$allergyNote$certificationNote"
        }
    }

    private fun suggestFoodsFromPreferences() {
        binding.foodSearchInput.setText(dietSearchTerm)
        searchFoods()
    }

    private fun searchFoods() {
        val query = binding.foodSearchInput.text.toString().trim()
        if (query.isBlank()) {
            binding.foodSearchInput.error = getString(R.string.search_food)
            return
        }

        // Key is configured by the developer in local.properties, not entered by each user.
        val apiKey = BuildConfig.USDA_API_KEY
        if (apiKey.isBlank()) {
            showMessage("Food search is not configured in this development build.")
            return
        }

        binding.foodResultsContainer.removeAllViews()
        addMessage("Searching…")
        binding.foodSearchButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = FoodDataClient.api.searchFoods(query, apiKey)
                binding.foodResultsContainer.removeAllViews()

                if (response.isSuccessful) {
                    val foods = response.body()?.foods.orEmpty()
                    if (foods.isEmpty()) addMessage("No foods found. Try another search.")
                    foods.forEach { addFoodCard(it) }
                } else {
                    addMessage("Food search failed (${response.code()}). Please try again.")
                }
            } catch (_: Exception) {
                binding.foodResultsContainer.removeAllViews()
                addMessage("Could not reach FoodData Central. Check your internet connection.")
            } finally {
                binding.foodSearchButton.isEnabled = true
            }
        }
    }

    private fun showMessage(message: String) {
        binding.foodResultsContainer.removeAllViews()
        addMessage(message)
    }

    private fun addMessage(message: String) {
        val text = TextView(this).apply {
            this.text = message
            setPadding(12, 16, 12, 16)
        }
        binding.foodResultsContainer.addView(text)
    }

    private fun addFoodCard(food: FoodItem) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xFFF2F5F1.toInt())
        }

        val title = TextView(this).apply {
            text = food.description
            textSize = 16f
            setTextColor(0xFF202020.toInt())
        }

        val brand = food.brandName?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
        val calories = TextView(this).apply {
            text = "${food.calories().toInt()} kcal per 100 g$brand"
            setPadding(0, 6, 0, 8)
        }

        val add = Button(this).apply {
            text = "Add to meal list"
            setOnClickListener { chooseMeal(food) }
        }

        card.addView(title)
        card.addView(calories)
        card.addView(add)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 12)
        binding.foodResultsContainer.addView(card, params)
    }

    private fun chooseMeal(food: FoodItem) {
        val meals = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
        AlertDialog.Builder(this)
            .setTitle("Add ${food.description} to…")
            .setItems(meals) { _, which -> askQuantity(food, meals[which]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askQuantity(food: FoodItem, mealType: String) {
        val input = EditText(this).apply {
            hint = "Quantity in grams (e.g. 150)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("100")
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("How much did you plan?")
            .setView(input)
            .setPositiveButton("Add to $mealType") { _, _ ->
                val grams = input.text.toString().toIntOrNull()
                if (grams == null || grams !in 1..3000) {
                    Toast.makeText(this, "Enter a quantity between 1 and 3000 g.", Toast.LENGTH_LONG).show()
                } else saveMeal(food, mealType, grams)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMeal(food: FoodItem, mealType: String, grams: Int) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show()
            return
        }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val caloriesPer100g = food.calories().toInt()
        val calories = (caloriesPer100g * grams / 100.0).toInt()
        val entry = hashMapOf<String, Any>(
            "foodId" to food.fdcId,
            "name" to food.description,
            "caloriesPer100g" to caloriesPer100g,
            "servingGrams" to grams,
            "calories" to calories,
            "date" to date,
            "mealType" to mealType,
            "status" to "planned",
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("mealLogs")
            .document("${date}_${UUID.randomUUID()}").set(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Added $grams g to $mealType. Mark it eaten when you have it.", Toast.LENGTH_LONG).show()
                showMealScheduledNotification(mealType)
                loadTodayMeals()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Could not save meal: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showMealScheduledNotification(mealType: String) {
        val prefs = getSharedPreferences("home_fitness_settings", MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", false)) return
        val key = when (mealType) { "Breakfast" -> "breakfast_time"; "Lunch" -> "lunch_time"; "Dinner" -> "dinner_time"; else -> return }
        val time = prefs.getString(key, "07:00") ?: "07:00"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("home_fitness_updates", "Home Fitness updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(this, "home_fitness_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$mealType scheduled")
            .setContentText("Your $mealType is scheduled for $time.")
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(this).notify(mealType.hashCode(), notification) } catch (_: SecurityException) {}
    }

    private fun loadTodayMeals() {
        val uid = auth.currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val container = findViewById<LinearLayout>(R.id.todayMealsContainer) ?: return
        db.collection("users").document(uid).collection("mealLogs")
            .whereEqualTo("date", date).get().addOnSuccessListener { docs ->
                container.removeAllViews()
                docs.documents.forEach { doc ->
                    val name = doc.getString("name") ?: "Food"
                    val meal = doc.getString("mealType") ?: "Meal"
                    val grams = doc.getLong("servingGrams") ?: 100L
                    val calories = doc.getLong("calories") ?: 0L
                    val eaten = doc.getString("status") != "planned"
                    val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12); setBackgroundColor(0xFFF0F5EC.toInt()) }
                    row.addView(TextView(this).apply { text = "$meal • $name"; textSize = 15f; setTextColor(0xFF203326.toInt()) })
                    row.addView(TextView(this).apply { text = "$grams g • $calories kcal • ${if (eaten) "Eaten" else "Planned"}"; textSize = 12f; setPadding(0, 4, 0, 4) })
                    if (!eaten) {
                        row.addView(Button(this).apply {
                            text = "Mark as eaten"
                            setOnClickListener {
                                doc.reference.update("status", "eaten", "eatenAt", FieldValue.serverTimestamp())
                                    .addOnSuccessListener { loadTodayCalories(); loadTodayMeals() }
                            }
                        })
                    } else {
                        row.addView(Button(this).apply {
                            text = "Remove from diary"
                            setOnClickListener { doc.reference.delete().addOnSuccessListener { loadTodayCalories(); loadTodayMeals() } }
                        })
                    }
                    val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 8)
                    container.addView(row, lp)
                }
                if (docs.isEmpty) container.addView(TextView(this).apply { text = "No meals added yet."; setPadding(8, 12, 8, 12) })
            }
    }

    private fun loadTodayCalories() {
        val uid = auth.currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        db.collection("users").document(uid).collection("mealLogs")
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                val total = documents.documents.filter { it.getString("status") != "planned" }.sumOf { it.getLong("calories") ?: 0L }
                val target = getSharedPreferences("home_fitness_settings", MODE_PRIVATE).getInt("calorie_target", 2000)
                binding.todayCaloriesText.text = "Calories eaten today: $total / $target kcal"
            }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) { loadTodayCalories(); loadTodayMeals() }
    }
}
