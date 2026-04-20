package com.example.nextstep

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.content.res.ResourcesCompat
import com.example.nextstep.ui.setOnClickWithSound
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class EmpathyResponseActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContent: LinearLayout
    private lateinit var tvMessage: TextView
    private lateinit var llOptionsContainer: LinearLayout

    // Score overlay UI elements
    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var gameRunning = false
    private var empathyScenarios: List<EmpathyScenario> = emptyList()
    private var currentScenario: EmpathyScenario? = null
    private val askedIndices = mutableSetOf<Int>()

    private val GAME_DURATION_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())

    // MediaPlayers for sounds
    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empathy_response)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContent = findViewById(R.id.llChallengeContent)
        tvMessage = findViewById(R.id.tvMessage)
        llOptionsContainer = findViewById(R.id.llOptionsContainer)

        // Inflate final score layout dynamically and add to root
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

        btnStartGame.setOnClickListener {
            playClickSound()
            startGame()
        }
        btnBackToDashboard.setOnClickListener {
            playClickSound()
            startActivity(Intent(this, HomeDashboardActivity::class.java))
            finish()
        }

        loadEmpathyScenarios()
        resetGameUI()
    }

    private fun loadEmpathyScenarios() {
        try {
            val jsonString = assets.open("empathy_response_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<EmpathyScenario>>() {}.type
            empathyScenarios = Gson().fromJson(jsonString, type)
            if (empathyScenarios.isEmpty()) {
                Toast.makeText(this, "No empathy scenarios found.", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
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
        tvInstructions.text = "Choose the most empathetic response! Tap 'Start Game' to begin."
        tvInstructions.setTextColor(ResourcesCompat.getColor(resources, R.color.text_medium, theme))
        tvInstructions.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.VISIBLE
        btnBackToDashboard.visibility = View.VISIBLE
        llChallengeContent.visibility = View.GONE
        llOptionsContainer.removeAllViews()
        scoreLayout.visibility = View.GONE
        score = 0
        gameRunning = false
        askedIndices.clear()
    }

    private fun startGame() {
        if (empathyScenarios.isEmpty()) {
            Toast.makeText(this, "Game data not loaded.", Toast.LENGTH_SHORT).show()
            return
        }
        score = 0
        gameRunning = true
        askedIndices.clear()
        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        llChallengeContent.visibility = View.VISIBLE

        startTimer()
        displayNextScenario()
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

    private fun displayNextScenario() {
        if (!gameRunning) return

        val availableIndices = empathyScenarios.indices.filter { it !in askedIndices }
        if (availableIndices.isEmpty()) {
            endGame()
            return
        }
        val selectedIndex = availableIndices.random()
        askedIndices.add(selectedIndex)
        currentScenario = empathyScenarios[selectedIndex]

        currentScenario?.let { scenario ->
            tvMessage.text = scenario.message
            llOptionsContainer.removeAllViews()
            scenario.responseOptions.shuffled().forEach { option ->
                val btnOption = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 8, 0, 8) }
                    text = option
                    setBackgroundResource(R.drawable.rounded_button_background)
                    setTextColor(resources.getColor(R.color.button_text_light, theme))
                    setOnClickListener {
                        handleAnswer(option)
                    }
                }
                llOptionsContainer.addView(btnOption)
            }
        }
    }

    private fun handleAnswer(selectedOption: String) {
        if (!gameRunning) return

        currentScenario?.let { scenario ->
            val isCorrect = selectedOption == scenario.correctResponse

            if (isCorrect) {
                score++
                playCorrectSound()
                tvFeedback.text = "Correct! +1 point."
                tvScore.text = "Score: $score"
                tvFeedback.setTextColor(resources.getColor(R.color.success_green, theme))
            } else {
                playWrongSound()
                tvFeedback.text = "Incorrect. The correct response was: ${scenario.correctResponse}"
                tvFeedback.setTextColor(resources.getColor(R.color.error_red, theme))
            }

            tvFeedback.visibility = View.VISIBLE
            llOptionsContainer.children.filterIsInstance<Button>().forEach { it.isEnabled = false }

            handler.postDelayed({
                tvFeedback.visibility = View.GONE
                displayNextScenario()
            }, 2000)
        }
    }

    private fun endGame() {
        gameRunning = false
        countDownTimer?.cancel()
        llChallengeContent.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE

        tvFinalScore.text = getString(R.string.final_score_message, score)
        scoreLayout.visibility = View.VISIBLE
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
