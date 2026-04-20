package com.example.nextstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import com.example.nextstep.ui.setOnClickWithSound
import com.example.nextstep.ui.SoundType
import com.example.nextstep.ui.playSound

class GameActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvScenarioTitle: TextView
    private lateinit var tvScenarioDescription: TextView
    private lateinit var llChoicesContainer: LinearLayout
    private lateinit var tvOutcomeText: TextView
    private lateinit var tvPersonalityScores: TextView
    private lateinit var btnNextScenario: Button
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar

    private var scenarios: List<Scenario> = emptyList() // full list loaded from JSON
    private var roundScenarios: List<Scenario> = emptyList() // 10 for current round
    private var currentScenarioIndex = 0
    private var userPersonalityScores: MutableMap<String, Int> = mutableMapOf(
        "assertiveness" to 0,
        "empathy" to 0,
        "leadership" to 0,
        "adaptability" to 0,
        "analytical_thinking" to 0,
        "creativity" to 0,
        "stress_tolerance" to 0,
        "teamwork" to 0,
        "responsibility" to 0,
        "attention_to_detail" to 0,
        "problem_solving" to 0
    )
    private var currentUserName: String = "Explorer"

    // Tracking IDs for non-repeating rounds
    private val servedScenarioIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvScenarioTitle = findViewById(R.id.tvScenarioTitle)
        tvScenarioDescription = findViewById(R.id.tvScenarioDescription)
        llChoicesContainer = findViewById(R.id.llChoicesContainer)
        tvOutcomeText = findViewById(R.id.tvOutcomeText)
        tvPersonalityScores = findViewById(R.id.tvPersonalityScores)
        btnNextScenario = findViewById(R.id.btnNextScenario)
        tvProgress = findViewById(R.id.tvProgress)
        progressBar = findViewById(R.id.progressBar)

        loadScenarios()
        loadUserPersonalityScores()

        btnNextScenario.setOnClickWithSound {
            currentScenarioIndex++
            if (currentScenarioIndex < roundScenarios.size) {
                displayCurrentScenario()
            } else {
                endGame() // End after 10 questions answered
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserPersonalityScores()
    }

    private fun loadScenarios() {
        try {
            val jsonString = assets.open("game_scenarios.json").bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<List<Scenario>>() {}.type
            scenarios = com.google.gson.Gson().fromJson(jsonString, type) ?: emptyList()
            selectNewRoundScenarios()
        } catch (e: Exception) {
            Log.e("GameActivity", "Error loading scenarios: ${e.message}", e)
            Toast.makeText(this, "Error loading scenarios. Please try again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun selectNewRoundScenarios() {
        val remaining = scenarios.filter { it.id !in servedScenarioIds }
        roundScenarios = if (remaining.size <= 10) {
            remaining.shuffled()
        } else {
            remaining.shuffled().take(10)
        }
        servedScenarioIds.addAll(roundScenarios.map { it.id })
        if (servedScenarioIds.size == scenarios.size) {
            servedScenarioIds.clear() // reset so repeats now allowed
        }
        currentScenarioIndex = 0
        if (roundScenarios.isNotEmpty()) {
            displayCurrentScenario()
        } else {
            Toast.makeText(this, "No scenarios found.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadUserPersonalityScores() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUserName = document.getString("fullName") ?: "Explorer"
                    @Suppress("UNCHECKED_CAST")
                    val scoresFromDb = document.get("personalityScores") as? Map<String, Long>
                    scoresFromDb?.forEach { (trait, score) ->
                        userPersonalityScores[trait] = score.toInt()
                    }
                }
                updatePersonalityScoresDisplay()
                updateProgressUI()
            }
            .addOnFailureListener { e ->
                Log.e("GameActivity", "Failed to load personality scores: ${e.message}")
            }
    }

    private fun saveUserPersonalityScores() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("personalityScores", userPersonalityScores)
            .addOnFailureListener { e ->
                Log.e("GameActivity", "Failed to save personality scores: ${e.message}")
            }
    }

    private fun displayCurrentScenario() {
        if (currentScenarioIndex >= roundScenarios.size) {
            endGame()
            return
        }

        val current = roundScenarios[currentScenarioIndex]

        updateProgressUI()
        tvScenarioTitle.text = current.title
        tvScenarioDescription.text = current.description ?: ""

        btnNextScenario.visibility = View.GONE
        tvOutcomeText.visibility = View.GONE
        llChoicesContainer.removeAllViews()
        enableChoices(true)

        current.choices.forEach { choice ->
            val button = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                text = choice.text
                isAllCaps = false // disables default all caps on buttons
                setBackgroundResource(R.drawable.rounded_button_background)
                setTextColor(resources.getColor(R.color.button_text_light, theme))
                textSize = 16f
                setPadding(24, 12, 24, 12)
                setOnClickWithSound {
                    handleChoiceSelection(choice)
                }
            }
            llChoicesContainer.addView(button)
        }
    }

    private fun updateProgressUI() {
        if (roundScenarios.isEmpty()) return
        val progressPercent = ((currentScenarioIndex + 1) * 100) / roundScenarios.size
        tvProgress.text = "Question ${currentScenarioIndex + 1} of ${roundScenarios.size}"
        progressBar.progress = progressPercent
    }

    private fun handleChoiceSelection(selectedChoice: Choice) {
        playSound(SoundType.CORRECT)

        selectedChoice.traits.forEach { (trait, change) ->
            userPersonalityScores[trait] = (userPersonalityScores[trait] ?: 0) + change
        }
        saveUserPersonalityScores()
        updatePersonalityScoresDisplay()

        tvOutcomeText.text = selectedChoice.outcomeText
        tvOutcomeText.visibility = View.VISIBLE

        enableChoices(false)
        btnNextScenario.visibility = View.VISIBLE
    }

    private fun enableChoices(enable: Boolean) {
        for (i in 0 until llChoicesContainer.childCount) {
            val child = llChoicesContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = enable
                child.alpha = if (enable) 1.0f else 0.5f
            }
        }
    }

    private fun updatePersonalityScoresDisplay() {
        val scoresText = StringBuilder("Personality Scores:\n")
        userPersonalityScores.forEach { (trait, score) ->
            val formattedTrait = trait.replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }
            scoresText.append("$formattedTrait: $score\n")
        }
        tvPersonalityScores.text = scoresText.toString()
    }

    private fun endGame() {
        playSound(SoundType.CORRECT)
        val scoresJson = com.google.gson.Gson().toJson(userPersonalityScores)
        val intent = Intent(this, PersonalityResultActivity::class.java).apply {
            putExtra("PERSONALITY_SCORES_JSON", scoresJson)
            putExtra("USER_NAME", currentUserName)
        }
        startActivity(intent)
        finish()
    }
}
