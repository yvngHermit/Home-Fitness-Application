package com.example.homefitness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.example.homefitness.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("home_fitness_settings", MODE_PRIVATE) }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        binding.notificationsSwitch.isChecked = granted
        prefs.edit().putBoolean("notifications_enabled", granted).apply()
        if (!granted) Toast.makeText(this, getString(R.string.notification_permission_message), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val languages = listOf("en", "af", "zu")
        val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "en" }
        binding.languageSpinner.setSelection(languages.indexOfFirst { currentLanguage.startsWith(it) }.coerceAtLeast(0))
        val themes = listOf("system", "light", "dark")
        binding.themeSpinner.setSelection(themes.indexOf(prefs.getString("app_theme", "system")).coerceAtLeast(0))

        binding.calorieTargetInput.setText(prefs.getInt("calorie_target", 2000).toString())
        val units = listOf("kg", "lb")
        binding.weightUnitSpinner.setSelection(units.indexOf(prefs.getString("weight_unit", "kg")).coerceAtLeast(0))
        binding.notificationsSwitch.isChecked = prefs.getBoolean("notifications_enabled", false)
        binding.workoutReminderSwitch.isChecked = prefs.getBoolean("workout_reminders_enabled", false)

        setSpinner(binding.breakfastTimeSpinner, R.array.breakfast_times, "breakfast_time", "07:00")
        setSpinner(binding.lunchTimeSpinner, R.array.lunch_times, "lunch_time", "13:00")
        setSpinner(binding.dinnerTimeSpinner, R.array.dinner_times, "dinner_time", "18:00")
        setSpinner(binding.workoutTimeSpinner, R.array.workout_times, "workout_reminder_time", "17:00")

        binding.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else prefs.edit().putBoolean("notifications_enabled", checked).apply()
        }

        binding.saveSettingsButton.setOnClickListener {
            val target = binding.calorieTargetInput.text?.toString()?.toIntOrNull()
            if (target == null || target !in 500..10000) {
                binding.calorieTargetInput.error = getString(R.string.calorie_target_error)
                return@setOnClickListener
            }
            val language = languages[binding.languageSpinner.selectedItemPosition.coerceIn(0, 2)]
            val theme = themes[binding.themeSpinner.selectedItemPosition.coerceIn(0, 2)]
            prefs.edit()
                .putInt("calorie_target", target)
                .putString("weight_unit", units[binding.weightUnitSpinner.selectedItemPosition.coerceIn(0, 1)])
                .putString("app_theme", theme)
                .putString("breakfast_time", binding.breakfastTimeSpinner.selectedItem.toString())
                .putString("lunch_time", binding.lunchTimeSpinner.selectedItem.toString())
                .putString("dinner_time", binding.dinnerTimeSpinner.selectedItem.toString())
                .putString("workout_reminder_time", binding.workoutTimeSpinner.selectedItem.toString())
                .putBoolean("workout_reminders_enabled", binding.workoutReminderSwitch.isChecked)
                .apply()

            AppCompatDelegate.setDefaultNightMode(when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))

            MealReminderScheduler.scheduleAll(this)
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }
        binding.privacyInfoButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.privacy_information)
                .setMessage(R.string.privacy_message)
                .setPositiveButton(R.string.close, null)
                .show()
        }
        binding.settingsSignOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
        binding.backSettingsButton.setOnClickListener { finish() }
    }

    private fun setSpinner(spinner: android.widget.Spinner, arrayId: Int, key: String, fallback: String) {
        val values = resources.getStringArray(arrayId)
        val saved = prefs.getString(key, fallback)
        spinner.setSelection(values.indexOf(saved).coerceAtLeast(0))
    }
}
