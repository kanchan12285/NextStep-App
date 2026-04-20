package com.example.nextstep

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class NextStepApplication : Application() {

    companion object {
        private var instance: NextStepApplication? = null

        fun getInstance(): NextStepApplication? {
            return instance
        }

        fun setAppLanguage(context: Context, languageCode: String) {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putString("app_language", languageCode).apply()

            // Update global locale immediately
            updateGlobalLocale(context, languageCode)
        }

        fun getAppLanguage(context: Context): String {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return prefs.getString("app_language", getSystemLanguage()) ?: "en"
        }

        private fun getSystemLanguage(): String {
            val systemLang = Locale.getDefault().language
            return when (systemLang) {
                "hi" -> "hi"
                "es" -> "es"
                else -> "en"
            }
        }

        private fun updateGlobalLocale(context: Context, languageCode: String) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Apply saved language on app start
        val savedLanguage = getAppLanguage(this)
        updateAppLanguage(savedLanguage)
    }

    override fun attachBaseContext(base: Context) {
        val languageCode = getAppLanguage(base)
        val context = updateContextLocale(base, languageCode)
        super.attachBaseContext(context)
    }

    private fun updateAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
