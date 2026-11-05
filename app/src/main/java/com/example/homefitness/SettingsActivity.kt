package com.example.homefitness

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.homefitness.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences("home_fitness_settings", MODE_PRIVATE)
        val tags = listOf("en", "af", "zu")
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "en" }
        binding.languageSpinner.setSelection(tags.indexOfFirst { current.startsWith(it) }.coerceAtLeast(0))
        binding.usdaApiKeyInput.setText(prefs.getString("usda_api_key", ""))
        binding.saveSettingsButton.setOnClickListener {
            prefs.edit().putString("usda_api_key", binding.usdaApiKeyInput.text.toString().trim()).apply()
            val tag = tags[binding.languageSpinner.selectedItemPosition]
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            Toast.makeText(this, getString(R.string.save), Toast.LENGTH_SHORT).show()
            recreate()
        }
        binding.getApiKeyText.setOnClickListener {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://fdc.nal.usda.gov/api-guide.html")))
        }
        binding.backSettingsButton.setOnClickListener { finish() }
    }
}
