package com.example.nextstep

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class FactCheckerActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContent: LinearLayout
    private lateinit var tvStatement: TextView
    private lateinit var btnFact: Button
    private lateinit var btnFiction: Button

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
    private var factCheckChallenges: List<FactCheckChallenge> = emptyList()
    private var currentChallenge: FactCheckChallenge? = null
    private val askedIndices = mutableSetOf<Int>()

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "FactCheckerActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fact_checker)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContent = findViewById(R.id.llChallengeContent)
        tvStatement = findViewById(R.id.tvStatement)
        btnFact = findViewById(R.id.btnFact)
        btnFiction = findViewById(R.id.btnFiction)

        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        // Inflate common final score layout dynamically
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)
        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)

        btnStartGame.setOnClickListener {
            playClickSound()
            startGame()
        }
        btnBackToDashboard.setOnClickListener {
            playClickSound()
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

        loadFactCheckChallenges()
        resetGameUI()
    }

    private fun loadFactCheckChallenges() {
        try {
            val jsonString = assets.open("fact_checker_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<FactCheckChallenge>>() {}.type
            factCheckChallenges = Gson().fromJson(jsonString, type)
            if (factCheckChallenges.isEmpty()) {
                Toast.makeText(this, "No fact check challenges found.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "fact_checker_data.json empty or parse error.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading fact check challenges: ${e.message}")
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
        scoreLayout.visibility = View.GONE
        llChallengeContent.visibility = View.GONE
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
        if (factCheckChallenges.isEmpty()) {
            Toast.makeText(this, "Game data not loaded.", Toast.LENGTH_SHORT).show()
            return
        }
        score = 0
        gameActive = true
        askedIndices.clear()

        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        llChallengeContent.visibility = View.VISIBLE

        startTimer()
        displayNextChallenge()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object: CountDownTimer(GAME_DURATION_MS,1000) {
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

        val availableIndices = factCheckChallenges.indices.filter { it !in askedIndices }
        if (availableIndices.isEmpty()) {
            endGame()
            return
        }
        val selectedIndex = availableIndices.random()
        askedIndices.add(selectedIndex)
        currentChallenge = factCheckChallenges[selectedIndex]

        // Re-enable buttons for new challenge
        btnFact.isEnabled = true
        btnFiction.isEnabled = true

        currentChallenge?.let { challenge ->
            tvStatement.text = challenge.statement

            btnFact.setOnClickListener { handleAnswer(true) }
            btnFiction.setOnClickListener { handleAnswer(false) }
        }
    }

    private fun handleAnswer(userSaysFact: Boolean) {
        if (!gameActive) return

        currentChallenge?.let { challenge ->
            val correct = userSaysFact == challenge.isFact

            if (correct) {
                score += 1
                playCorrectSound()
                tvFeedback.text = "Correct! +1 point"
                tvScore.text = "Score: $score"
                tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.success_green, theme))
            } else {
                playWrongSound()
                tvFeedback.text = "Incorrect! The correct answer was ${if (challenge.isFact) "FACT" else "FICTION"}."
                tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.error_red, theme))
            }

            tvFeedback.visibility = View.VISIBLE

            btnFact.isEnabled = false
            btnFiction.isEnabled = false

            handler.postDelayed({
                tvFeedback.visibility = View.GONE
                displayNextChallenge()
            }, 1800)
        }
    }

    private fun endGame() {
        gameActive = false
        countDownTimer?.cancel()
        llChallengeContent.visibility = View.GONE

        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = "Your final score: $score"

        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
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
