package com.example.nextstep

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import com.example.nextstep.ui.setOnClickWithSound
import com.example.nextstep.ui.QuizSoundHelper

class SymptomMatcherActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContainer: LinearLayout

    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var gameRunning = false
    private var symptomCases: List<SymptomCase> = emptyList()
    private var currentCase: SymptomCase? = null
    private var currentChallengeView: View? = null
    private val askedQuestionIds = mutableSetOf<String>()

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "SymptomMatcherActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom_matcher)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContainer = findViewById(R.id.llChallengeContent)

        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val scoreView = layoutInflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)

        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)

        btnPlayAgain.setOnClickWithSound {
            playClickSound()
            scoreLayout.visibility = View.GONE
            resetGameUI()
        }
        btnExit.setOnClickWithSound {
            playClickSound()
            finish()
        }

        loadSymptomCases()

        btnStartGame.setOnClickWithSound { startGame() }
        btnBackToDashboard.setOnClickWithSound {
            playClickSound()
            startActivity(Intent(this, HomeDashboardActivity::class.java))
            finish()
        }

        resetGameUI()
    }

    private fun loadSymptomCases() {
        try {
            val json = assets.open("symptom_matcher_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SymptomCase>>() {}.type
            symptomCases = Gson().fromJson(json, type)
            if (symptomCases.isEmpty()) {
                Toast.makeText(this, "No symptom cases found. Check your JSON.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "symptom_matcher_data.json empty or unparseable.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading cases: ${e.message}")
            Toast.makeText(this, "Error loading game data.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cases: ${e.message}")
            Toast.makeText(this, "Error parsing game data.", Toast.LENGTH_LONG).show()
        }
    }
    private fun updateUserProgress(finalScore: Int) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.get().addOnSuccessListener { document ->
                val prevGames = document.getLong("totalGamesPlayed") ?: 0
                val prevScore = document.getLong("bestScore") ?: 0
                val newBest = if (finalScore > prevScore) finalScore else prevScore

                userRef.update(
                    mapOf(
                        "totalGamesPlayed" to prevGames + 1,
                        "bestScore" to newBest
                    )
                )
            }
        }
    }

    private fun resetGameUI() {
        tvTimer.text = "Time: ${GAME_DURATION_MS / 1000}s"
        tvScore.text = "Score: 0"
        tvInstructions.text = getString(R.string.instructions_symptom_matcher)
        tvInstructions.setTextColor(resources.getColor(R.color.text_medium, theme))
        tvInstructions.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE
        btnStartGame.text = getString(R.string.start_game)
        btnStartGame.visibility = View.VISIBLE
        btnBackToDashboard.visibility = View.VISIBLE
        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.GONE
        scoreLayout.visibility = View.GONE
        score = 0
        gameRunning = false
        currentCase = null
        currentChallengeView = null
        askedQuestionIds.clear()
    }

    private fun startGame() {
        if (symptomCases.isEmpty()) {
            Toast.makeText(this, "Game data not loaded. Cannot start game.", Toast.LENGTH_SHORT).show()
            return
        }

        score = 0
        gameRunning = true
        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        scoreLayout.visibility = View.GONE
        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.VISIBLE

        startTimer()
        displayNextCase()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(GAME_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Timer tick: ${millisUntilFinished}")
                tvTimer.text = "Time: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                Log.d(TAG, "Timer finished")
                tvTimer.text = "Time: 0s"
                endGame()
            }
        }.start()
    }

    private fun displayNextCase() {
        if (!gameRunning) return
        currentChallengeView?.let { llChallengeContainer.removeView(it) }

        val availableCases = symptomCases.filter { it.id !in askedQuestionIds }
        if (availableCases.isEmpty()) {
            askedQuestionIds.clear() // Reset question tracker to allow looping
            displayNextCase()         // Call again to show questions repeatedly
            return
        }

        currentCase = availableCases.random()
        currentCase?.let { case ->
            askedQuestionIds.add(case.id)
            val inflated = tryInflateSymptomLayout(case)
            val viewToUse = inflated ?: buildSymptomLayoutProgrammatically(case)

            llChallengeContainer.addView(viewToUse)
            currentChallengeView = viewToUse
        }
    }

    private fun tryInflateSymptomLayout(c: SymptomCase): View? {
        return try {
            val v = LayoutInflater.from(this).inflate(R.layout.item_symptom_case, llChallengeContainer, false)

            val idSymptoms = resources.getIdentifier("tvPatientSymptoms", "id", packageName)
            val idOptions = resources.getIdentifier("llOptionsContainer", "id", packageName)

            val tvSymptoms = v.findViewById<TextView?>(idSymptoms)
            val llOptions = v.findViewById<LinearLayout?>(idOptions)

            if (tvSymptoms == null || llOptions == null) {
                null
            } else {
                tvSymptoms.text = "Symptoms: ${c.patientSymptoms.joinToString(", ")}"
                llOptions.removeAllViews()
                c.diagnosisOptions.shuffled().forEach { option ->
                    llOptions.addView(makeDiagnosisOptionButton(option))
                }
                v
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Inflate fallback (symptom): ${ex.message}")
            null
        }
    }

    private fun buildSymptomLayoutProgrammatically(c: SymptomCase): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32)
        }

        val tvSymptoms = TextView(this).apply {
            text = "Symptoms: ${c.patientSymptoms.joinToString(", ")}"
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_light, theme))
        }
        root.addView(tvSymptoms)

        val options = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.diagnosisOptions.shuffled().forEach { option ->
            options.addView(makeDiagnosisOptionButton(option))
        }
        root.addView(options)

        return root
    }

    private fun makeDiagnosisOptionButton(option: String): Button =
        Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
            text = option
            setBackgroundResource(R.drawable.rounded_button_background)
            setTextColor(resources.getColor(R.color.button_text_light, theme))
            setOnClickWithSound { handleAnswer(option) }
        }

    private fun handleAnswer(selectedOption: String) {
        if (!gameRunning) return

        currentCase?.let { case ->
            val isCorrect = selectedOption == case.correctDiagnosis

            QuizSoundHelper.handleQuizAnswer(isCorrect) { correct ->
                if (correct) {
                    score += 1
                    tvScore.text = "Score: $score"
                    tvFeedback.text = getString(R.string.feedback_correct, 1)
                    tvFeedback.setTextColor(resources.getColor(R.color.success_green, theme))
                    tvFeedback.visibility = View.VISIBLE
                    updateUserScoreInFirestore(1)
                    playCorrectSound()
                } else {
                    tvFeedback.text = getString(R.string.feedback_incorrect_symptom_matcher, case.correctDiagnosis, case.explanation)
                    tvFeedback.setTextColor(resources.getColor(R.color.error_red, theme))
                    tvFeedback.visibility = View.VISIBLE
                    updateUserScoreInFirestore(0)
                    playWrongSound()
                }

                currentChallengeView
                    ?.findViewById<LinearLayout>(resources.getIdentifier("llOptionsContainer", "id", packageName))
                    ?.children?.filterIsInstance<Button>()?.forEach { it.isEnabled = false }

                handler.postDelayed({
                    tvFeedback.visibility = View.GONE
                    displayNextCase()
                }, 1500)
            }
        }
    }

    private fun updateUserScoreInFirestore(points: Int) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .update("personalityScores.problem_solving", FieldValue.increment(points.toLong()))
                .addOnSuccessListener { Log.d(TAG, "Problem Solving score updated by $points") }
                .addOnFailureListener { e -> Log.e(TAG, "Error updating score: ${e.message}") }
        }
    }

    private fun endGame() {
        gameRunning = false
        countDownTimer?.cancel()
        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        llChallengeContainer.visibility = View.GONE

        // Hide gameplay UI
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        // Show score overlay with final score
        tvFinalScore.text = getString(R.string.final_score_message, score)
        scoreLayout.visibility = View.VISIBLE
        updateUserProgress(score)

    }

    private fun playClickSound() {
        if (::mediaPlayerClick.isInitialized) {
            if (mediaPlayerClick.isPlaying) mediaPlayerClick.seekTo(0)
            mediaPlayerClick.start()
        }
    }

    private fun playCorrectSound() {
        if (::mediaPlayerCorrect.isInitialized) {
            if (mediaPlayerCorrect.isPlaying) mediaPlayerCorrect.seekTo(0)
            mediaPlayerCorrect.start()
        }
    }

    private fun playWrongSound() {
        if (::mediaPlayerWrong.isInitialized) {
            if (mediaPlayerWrong.isPlaying) mediaPlayerWrong.seekTo(0)
            mediaPlayerWrong.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (::mediaPlayerClick.isInitialized) mediaPlayerClick.release()
        if (::mediaPlayerCorrect.isInitialized) mediaPlayerCorrect.release()
        if (::mediaPlayerWrong.isInitialized) mediaPlayerWrong.release()
        handler.removeCallbacksAndMessages(null)
    }
}
