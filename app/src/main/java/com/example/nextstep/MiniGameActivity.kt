package com.example.nextstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class MiniGameActivity : BaseActivity() {

    private val TAG = "MiniGameActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MiniGameActivity started")
        try {
            val gameId = intent.getStringExtra("GAME_ID")
            if (gameId == null) {
                showErrorAndFinish("Error: No game ID provided.")
                return
            }
            val activityClass = when (gameId) {
                "code_debugger" -> CodeDebuggerActivity::class.java
                "slogan_matcher" -> SloganMatcherActivity::class.java
                "market_predictor" -> MarketPredictorActivity::class.java
                "headline_huddle" -> HeadlineHuddleActivity::class.java
                "symptom_matcher" -> SymptomMatcherActivity::class.java
                "resource_balancer" -> ResourceBalancerActivity::class.java
                "empathy_response" -> EmpathyResponseActivity::class.java
                "color_palette_mixer" -> ColorPaletteMixerActivity::class.java
                "lesson_plan_organizer" -> LessonPlanOrganizerActivity::class.java
                "threat_identifier" -> ThreatIdentifierActivity::class.java
                "fact_checker" -> FactCheckerActivity::class.java
                else -> {
                    showErrorAndFinish("Error: Unknown game ID: $gameId")
                    return
                }
            }
            startActivity(Intent(this, activityClass))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error launching mini game", e)
            showErrorAndFinish("Error starting game: ${e.message}")
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
