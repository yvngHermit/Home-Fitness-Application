package com.example.homefitness

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class HomeFitnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val selectedTheme = getSharedPreferences("home_fitness_settings", MODE_PRIVATE)
            .getString("app_theme", "system") ?: "system"

        MealReminderScheduler.scheduleAll(this)

        AppCompatDelegate.setDefaultNightMode(
            when (selectedTheme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
