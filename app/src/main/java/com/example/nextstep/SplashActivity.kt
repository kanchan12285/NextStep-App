package com.example.nextstep

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Locale

class SplashActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "en") ?: "en"
        val context = setAppLocale(newBase, lang)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        // Ensure a user exists before any Firestore access
        val user = auth.currentUser
        if (user == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { goHome() }
                .addOnFailureListener { goHome() } // fail-open for now; handle UI as needed
        } else {
            goHome()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeDashboardActivity::class.java))
        finish()
    }

    private fun setAppLocale(context: Context, localeCode: String): Context {
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
