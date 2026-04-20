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

class SloganMatcherActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContainer: LinearLayout

    // Score overlay UI
    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var gameRunning = false
    private var sloganChallenges: List<SloganChallenge> = emptyList()
    private var currentChallenge: SloganChallenge? = null
    private var currentChallengeView: View? = null

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "SloganMatcherActivity"
    private val handler = Handler(Looper.getMainLooper())

    // MediaPlayers for sounds
    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slogan_matcher)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContainer = findViewById(R.id.llChallengeContent) // Confirm this ID in your XML

        // Inflate final score layout dynamically
        val rootView = findViewById<View>(android.R.id.content) as ViewGroup
        val scoreView = layoutInflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)

        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)
        scoreLayout.visibility = View.GONE

        btnPlayAgain.setOnClickWithSound {
            playClickSound()
            scoreLayout.visibility = View.GONE
            resetGameUI()
        }

        btnExit.setOnClickWithSound {
            playClickSound()
            finish()
        }

        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        btnStartGame.setOnClickWithSound { startGame() }
        btnBackToDashboard.setOnClickWithSound {
            playClickSound()
            startActivity(Intent(this, HomeDashboardActivity::class.java))
            finish()
        }

        loadSloganChallenges()
        resetGameUI()
    }

    private fun loadSloganChallenges() {
        try {
            val json = assets.open("slogan_matcher_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SloganChallenge>>() {}.type
            sloganChallenges = Gson().fromJson(json, type)
            if (sloganChallenges.isEmpty()) {
                Toast.makeText(this, "No slogan challenges found. Check slogan_matcher_data.json", Toast.LENGTH_LONG).show()
                Log.e(TAG, "slogan_matcher_data.json is empty or unparseable.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading challenges: ${e.message}")
            Toast.makeText(this, "Error loading game data.", Toast.LENGTH_LONG).show()
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
        tvInstructions.text = getString(R.string.instructions_slogan_matcher)
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
        currentChallenge = null
        currentChallengeView = null
    }

    private fun startGame() {
        if (sloganChallenges.isEmpty()) {
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
        llChallengeContainer.visibility = View.VISIBLE
        displayRandomChallenge()
        startTimer()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(GAME_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "Time: ${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                tvTimer.text = "Time: 0s"
                endGame()
            }
        }.start()
    }

    private fun displayRandomChallenge() {
        if (!gameRunning || sloganChallenges.isEmpty()) return

        currentChallengeView?.let { llChallengeContainer.removeView(it) }

        currentChallenge = sloganChallenges.shuffled().firstOrNull() ?: run {
            endGame()
            return
        }

        currentChallenge?.let { challenge ->
            val inflated = tryInflateSloganLayout(challenge)
            val viewToUse = inflated ?: buildSloganLayoutProgrammatically(challenge)
            llChallengeContainer.addView(viewToUse)
            currentChallengeView = viewToUse
        }
    }

    private fun tryInflateSloganLayout(c: SloganChallenge): View? {
        return try {
            val v = LayoutInflater.from(this).inflate(R.layout.item_slogan_challenge, llChallengeContainer, false)

            val idCompany = resources.getIdentifier("tvCompanyName", "id", packageName)
            val idDesc = resources.getIdentifier("tvDescription", "id", packageName)
            val idOptions = resources.getIdentifier("llOptionsContainer", "id", packageName)

            val tvCompany = v.findViewById<TextView?>(idCompany)
            val tvDesc = v.findViewById<TextView?>(idDesc)
            val llOptions = v.findViewById<LinearLayout?>(idOptions)

            if (tvCompany == null || tvDesc == null || llOptions == null) {
                null
            } else {
                tvCompany.text = c.companyName
                tvDesc.text = c.description
                llOptions.removeAllViews()
                c.sloganOptions.shuffled().forEach { option ->
                    llOptions.addView(makeSloganOptionButton(option))
                }
                v
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Inflate fallback (slogan): ${ex.message}")
            null
        }
    }

    private fun buildSloganLayoutProgrammatically(c: SloganChallenge): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32)
        }

        val tvCompany = TextView(this).apply {
            text = c.companyName
            textSize = 18f
            setTextColor(resources.getColor(R.color.text_light, theme))
        }
        root.addView(tvCompany)

        val tvDesc = TextView(this).apply {
            text = c.description
            textSize = 15f
            setTextColor(resources.getColor(R.color.text_medium, theme))
        }
        root.addView(tvDesc)

        val options = LinearLayout(this)
        options.orientation = LinearLayout.VERTICAL
        c.sloganOptions.shuffled().forEach { option ->
            options.addView(makeSloganOptionButton(option))
        }
        root.addView(options)
        return root
    }

    private fun makeSloganOptionButton(option: String): Button =
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

        currentChallenge?.let { challenge ->
            val isCorrect = selectedOption == challenge.correctSlogan

            QuizSoundHelper.handleQuizAnswer(isCorrect) { correct ->
                if (correct) {
                    score += 1  // Award 1 point per correct answer
                    tvScore.text = "Score: $score"
                    tvFeedback.text = getString(R.string.feedback_correct, 1)
                    tvFeedback.setTextColor(resources.getColor(R.color.success_green, theme))
                    tvFeedback.visibility = View.VISIBLE
                    updateUserScoreInFirestore(1)
                    playCorrectSound()
                } else {
                    tvFeedback.text = getString(
                        R.string.feedback_incorrect_slogan_matcher,
                        challenge.companyName,
                        challenge.correctSlogan
                    )
                    tvFeedback.setTextColor(resources.getColor(R.color.error_red, theme))
                    tvFeedback.visibility = View.VISIBLE
                    updateUserScoreInFirestore(0) // No penalty
                    playWrongSound()
                }

                currentChallengeView
                    ?.findViewById<LinearLayout?>(resources.getIdentifier("llOptionsContainer", "id", packageName))
                    ?.children?.filterIsInstance<Button>()?.forEach { it.isEnabled = false }

                handler.postDelayed({
                    tvFeedback.visibility = View.GONE
                    displayRandomChallenge()
                }, 2000)
            }
        }
    }

    private fun updateUserScoreInFirestore(points: Int) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .update("personalityScores.creativity", FieldValue.increment(points.toLong()))
                .addOnSuccessListener { Log.d(TAG, "Creativity score updated by $points") }
                .addOnFailureListener { e -> Log.e(TAG, "Error updating creativity score: ${e.message}") }
        }
    }

    private fun endGame() {
        gameRunning = false
        countDownTimer?.cancel()
        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        llChallengeContainer.visibility = View.GONE

        // Hide all gameplay UI
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        // Show final score overlay layout
        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = getString(R.string.final_score_message, score)
        updateUserProgress(score)

    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (::mediaPlayerClick.isInitialized) mediaPlayerClick.release()
        if (::mediaPlayerCorrect.isInitialized) mediaPlayerCorrect.release()
        if (::mediaPlayerWrong.isInitialized) mediaPlayerWrong.release()
        handler.removeCallbacksAndMessages(null)
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
}
