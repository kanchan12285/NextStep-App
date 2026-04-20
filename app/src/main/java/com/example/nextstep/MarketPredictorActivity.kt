package com.example.nextstep

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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class MarketPredictorActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBack: Button
    private lateinit var llChallengeContainer: LinearLayout

    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private var mediaClick: MediaPlayer? = null
    private var mediaCorrect: MediaPlayer? = null
    private var mediaWrong: MediaPlayer? = null

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var gameActive = false

    private val askedIndices = mutableSetOf<Int>()
    private lateinit var challenges: List<MarketChallenge>
    private var currentChallengeIndex = -1
    private var currentChallengeView: View? = null

    private val GAME_DURATION_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "MarketPredictorActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_market_predictor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBack = findViewById(R.id.btnBackToDashboard)
        llChallengeContainer = findViewById(R.id.llChallengeContainer)

        mediaClick = MediaPlayer.create(this, R.raw.button_click)
        mediaCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)

        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)
        scoreLayout.visibility = View.GONE

        setupButtons()
        loadChallenges()
        resetUI()
    }

    private fun setupButtons() {
        btnStartGame.setOnClickListener {
            playClick()
            startGame()
        }
        btnBack.setOnClickListener {
            playClick()
            finish()
        }
        btnPlayAgain.setOnClickListener {
            playClick()
            scoreLayout.visibility = View.GONE
            resetUI()
        }
        btnExit.setOnClickListener {
            playClick()
            finish()
        }
    }

    private fun loadChallenges() {
        try {
            val json = assets.open("market_predictor_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<MarketChallenge>>() {}.type
            challenges = Gson().fromJson(json, type)
            if (challenges.isEmpty()) {
                Toast.makeText(this, "No challenges loaded.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No challenges found in JSON")
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error loading challenges.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error loading challenges: ${e.message}")
            challenges = emptyList()
        }
    }

    private fun resetUI() {
        score = 0
        gameActive = false
        askedIndices.clear()

        tvScore.text = "Score: 0"
        tvTimer.text = "Time: ${GAME_DURATION_MS / 1000}s"
        tvInstructions.text = "Answer the following prediction questions."
        tvInstructions.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.VISIBLE
        btnBack.visibility = View.VISIBLE
        llChallengeContainer.visibility = View.GONE
        scoreLayout.visibility = View.GONE
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

    private fun startGame() {
        if (challenges.isEmpty()) {
            Toast.makeText(this, "No challenges available to play.", Toast.LENGTH_SHORT).show()
            return
        }

        score = 0
        askedIndices.clear()
        gameActive = true
        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBack.visibility = View.GONE
        llChallengeContainer.visibility = View.VISIBLE

        startTimer()
        loadNextChallenge()
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

    private fun loadNextChallenge() {
        if (askedIndices.size == challenges.size) {
            endGame()
            return
        }

        var idx: Int
        do {
            idx = challenges.indices.random()
        } while (askedIndices.contains(idx))

        askedIndices.add(idx)
        currentChallengeIndex = idx
        showChallenge(challenges[idx])
    }

    private fun showChallenge(challenge: MarketChallenge) {
        currentChallengeView?.let { llChallengeContainer.removeView(it) }

        val view = LayoutInflater.from(this).inflate(R.layout.item_market_scenario, llChallengeContainer, false)
        currentChallengeView = view

        val tvScenario = view.findViewById<TextView>(R.id.tvScenarioDescription)
        val tvData = view.findViewById<TextView>(R.id.tvMarketData)
        val llOptions = view.findViewById<LinearLayout>(R.id.llOptions)

        tvScenario.text = challenge.scenario
        tvData.text = "Market data:\n" + challenge.marketData.joinToString("\n") { "• $it" }

        llOptions.removeAllViews()
        challenge.options.shuffled().forEach { option: String ->
            val btn = Button(this).apply {
                text = option
                setOnClickListener { submitAnswer(option) }
                setBackgroundResource(R.drawable.rounded_button_background)
                setTextColor(ResourcesCompat.getColor(resources, R.color.button_text_light, theme))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
            }
            llOptions.addView(btn)
        }

        llChallengeContainer.addView(view)
    }

    private fun submitAnswer(selected: String) {
        if (!gameActive) return

        val isCorrect = challenges[currentChallengeIndex].correctPrediction == selected

        if (isCorrect) {
            score++
            playCorrect()
            tvScore.text = "Score: $score"
            tvFeedback.text = "Correct! +1 point"
            tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.success_green, theme))
        } else {
            playWrong()
            tvFeedback.text = "Incorrect! Try next!"
            tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.error_red, theme))
        }

        tvFeedback.visibility = View.VISIBLE

        val llOptions = currentChallengeView?.findViewById<LinearLayout>(R.id.llOptions)
        llOptions?.children?.forEach { child: View ->
            (child as? Button)?.isEnabled = false
        }

        handler.postDelayed({
            tvFeedback.visibility = View.GONE
            if (gameActive) loadNextChallenge()
        }, 1800)
    }

    private fun endGame() {
        gameActive = false
        countDownTimer?.cancel()

        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.GONE

        tvInstructions.text = "Game Over! Your Score: $score"
        tvInstructions.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE

        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = "Your Score: $score"

        btnStartGame.text = "Play Again"
        btnStartGame.visibility = View.VISIBLE
        btnBack.visibility = View.VISIBLE

        updateUserProgress(score)  // << Add this line
    }


    private fun playClick() {
        mediaClick?.let {
            if (it.isPlaying) it.seekTo(0)
            it.start()
        }
    }

    private fun playCorrect() {
        mediaCorrect?.let {
            if (it.isPlaying) it.seekTo(0)
            it.start()
        }
    }

    private fun playWrong() {
        mediaWrong?.let {
            if (it.isPlaying) it.seekTo(0)
            it.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        mediaClick?.release()
        mediaCorrect?.release()
        mediaWrong?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
