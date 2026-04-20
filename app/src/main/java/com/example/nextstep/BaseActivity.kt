package com.example.nextstep

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * Base activity that all activities should extend
 * Provides automatic language setting and common functionality
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageCode = NextStepApplication.getAppLanguage(newBase)
        val context = updateContextLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure language is applied
        val currentLanguage = NextStepApplication.getAppLanguage(this)
        updateActivityLanguage(currentLanguage)
    }

    override fun onResume() {
        super.onResume()

        // Check if language changed while app was in background
        val savedLanguage = NextStepApplication.getAppLanguage(this)
        val currentLocale = resources.configuration.locales[0].language

        if (savedLanguage != currentLocale) {
            recreate() // Recreate activity to apply new language
        }
    }

    private fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun updateActivityLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Helper method to get localized string
     */
    protected fun getLocalizedString(resourceId: Int): String {
        return getString(resourceId)
    }

    /**
     * Helper method to change language and restart activity
     */
    protected fun changeLanguageAndRestart(languageCode: String) {
        NextStepApplication.setAppLanguage(this, languageCode)
        recreate()
    }
}
