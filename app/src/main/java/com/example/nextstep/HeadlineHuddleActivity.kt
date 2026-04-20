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
import com.example.nextstep.ui.QuizSoundHelper

class HeadlineHuddleActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContainer: LinearLayout
    private lateinit var tvArticleSummary: TextView
    private lateinit var llOptionsContainer: LinearLayout

    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var gameActive = false
    private var headlineChallenges: List<HeadlineChallenge> = emptyList()
    private var currentChallenge: HeadlineChallenge? = null
    private var currentChallengeView: View? = null
    private val askedIndices = mutableSetOf<Int>()

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "HeadlineHuddleActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_headline_huddle)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBack)
        llChallengeContainer = findViewById(R.id.llChallengeContainer)

        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)

        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)

        btnStartGame.setOnClickListener { startGame() }
        btnBackToDashboard.setOnClickListener {
            startActivity(Intent(this, HomeDashboardActivity::class.java))
            finish()
        }
        btnPlayAgain.setOnClickListener {
            playClickSound()
            scoreLayout.visibility = View.GONE
            resetGameUI()
        }
        btnExit.setOnClickListener {
            playClickSound()
            finish()
        }

        loadHeadlineChallenges()
        resetGameUI()
    }

    private fun loadHeadlineChallenges() {
        try {
            val jsonString = assets.open("headline_huddle_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<HeadlineChallenge>>() {}.type
            headlineChallenges = Gson().fromJson(jsonString, type)
            if (headlineChallenges.isEmpty()) {
                Toast.makeText(this, "No headline challenges found.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "headline_huddle_data.json is empty or could not be parsed.")
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error loading headline challenges.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error loading headline challenges: ${e.message}")
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
        scoreLayout.visibility = View.GONE
        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.VISIBLE
        btnBackToDashboard.visibility = View.VISIBLE
        tvInstructions.visibility = View.VISIBLE
        tvTimer.text = "Time: ${GAME_DURATION_MS / 1000}s"
        score = 0
        tvScore.text = "Score: 0"
        gameActive = false
        askedIndices.clear()
    }

    private fun startGame() {
        if (headlineChallenges.isEmpty()) {
            Toast.makeText(this, "Game data not loaded.", Toast.LENGTH_SHORT).show()
            return
        }

        score = 0
        gameActive = true
        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.VISIBLE

        startTimer()
        displayNextChallenge()
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

    private fun displayNextChallenge() {
        if (!gameActive) return

        currentChallengeView?.let { llChallengeContainer.removeView(it) }

        val availableIndices = headlineChallenges.indices.filter { it !in askedIndices }
        if (availableIndices.isEmpty()) {
            endGame()
            return
        }
        val selectedIndex = availableIndices.random()
        askedIndices.add(selectedIndex)
        currentChallenge = headlineChallenges[selectedIndex]

        currentChallenge?.let { challenge ->
            val challengeLayout = LayoutInflater.from(this).inflate(R.layout.item_headline_challenge, llChallengeContainer, false)

            tvArticleSummary = challengeLayout.findViewById(R.id.tvArticleSummary)
            llOptionsContainer = challengeLayout.findViewById(R.id.llOptionsContainer)

            tvArticleSummary.text = challenge.articleSummary
            llOptionsContainer.removeAllViews()

            challenge.headlineOptions.shuffled().forEach { option ->
                val optionBtn = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    text = option
                    setBackgroundResource(R.drawable.rounded_button_background)
                    setTextColor(resources.getColor(R.color.button_text_light, theme))

                    setOnClickListener {
                        handleAnswer(option)
                    }
                }
                llOptionsContainer.addView(optionBtn)
            }

            llChallengeContainer.addView(challengeLayout)
            currentChallengeView = challengeLayout
        } ?: endGame()
    }

    private fun handleAnswer(selectedOption: String) {
        if (!gameActive) return

        currentChallenge?.let { challenge ->
            val isCorrect = selectedOption == challenge.correctHeadline

            if (isCorrect) {
                score += 1
                playCorrectSound()
                tvFeedback.text = getString(R.string.feedback_correct, 1)
                tvScore.text = "Score: $score"
                tvFeedback.setTextColor(resources.getColor(R.color.success_green, theme))
            } else {
                playWrongSound()
                tvFeedback.text = getString(R.string.feedback_incorrect_headline_huddle, challenge.correctHeadline, challenge.explanation)
                tvFeedback.setTextColor(resources.getColor(R.color.error_red, theme))
            }

            tvFeedback.visibility = View.VISIBLE

            llOptionsContainer.children.filterIsInstance<Button>().forEach { it.isEnabled = false }

            handler.postDelayed({
                tvFeedback.visibility = View.GONE
                displayNextChallenge()
            }, 2000)
        }
    }

    private fun endGame() {
        gameActive = false
        countDownTimer?.cancel()
        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        llChallengeContainer.visibility = View.GONE

        // Hide gameplay UI
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        // Show score overlay with final score
        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = getString(R.string.final_score_message, score)
    }

    private fun playClickSound() {
        if (::mediaPlayerClick.isInitialized && mediaPlayerClick.isPlaying) mediaPlayerClick.seekTo(0)
        mediaPlayerClick.start()
    }

    private fun playCorrectSound() {
        if (::mediaPlayerCorrect.isInitialized && mediaPlayerCorrect.isPlaying) mediaPlayerCorrect.seekTo(0)
        mediaPlayerCorrect.start()
    }

    private fun playWrongSound() {
        if (::mediaPlayerWrong.isInitialized && mediaPlayerWrong.isPlaying) mediaPlayerWrong.seekTo(0)
        mediaPlayerWrong.start()
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
