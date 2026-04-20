package com.example.nextstep

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class CodeDebuggerActivity : BaseActivity() {

    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var llChallengeContainer: LinearLayout
    private lateinit var tvChallengeDescription: TextView
    private lateinit var tvBuggyCode: TextView
    private lateinit var llChallengeOptions: LinearLayout
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button

    // Final score layout views (inflated dynamically)
    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private lateinit var allChallenges: List<CodeChallenge>
    private val askedQuestionIds = mutableSetOf<String>()
    private var currentChallenge: CodeChallenge? = null
    private var currentScore = 0
    private var gameActive = false

    private lateinit var countDownTimer: CountDownTimer
    private val totalTimeMillis: Long = 30_000 // 30 seconds
    private val intervalMillis: Long = 1_000 // update every second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_debugger)

        // Find views in main game layout
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        llChallengeContainer = findViewById(R.id.llChallengeContainer)
        tvChallengeDescription = findViewById(R.id.tvChallengeDescription)
        tvBuggyCode = findViewById(R.id.tvBuggyCode)
        llChallengeOptions = findViewById(R.id.llChallengeOptions)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)

        // Inflate the final score layout dynamically and add it to the root view
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)
        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE

        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)

        // Prepare media players for sounds
        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        // Button listeners for main layout buttons
        btnStartGame.setOnClickListener {
            playClickSound()
            startNewGame()
        }
        btnBackToDashboard.setOnClickListener {
            playClickSound()
            finish()
        }

        // Button listeners for final score layout buttons
        btnPlayAgain.setOnClickListener {
            playClickSound()
            scoreLayout.visibility = View.GONE
            updateUIForStart()
        }
        btnExit.setOnClickListener {
            playClickSound()
            finish()
        }

        loadChallenges()
        updateUIForStart()
    }

    private fun updateUIForStart() {
        scoreLayout.visibility = View.GONE
        llChallengeContainer.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.VISIBLE
        tvInstructions.visibility = View.VISIBLE
        tvTimer.text = "Timer: 30"
        currentScore = 0
        tvScore.text = getString(R.string.score_placeholder, currentScore)
        askedQuestionIds.clear()
        gameActive = false
    }

    private fun loadChallenges() {
        val inputStream = assets.open("code_challenges.json")
        val reader = InputStreamReader(inputStream)
        val challengeType = object : TypeToken<List<CodeChallenge>>() {}.type
        allChallenges = Gson().fromJson(reader, challengeType)
        reader.close()
    }

    private fun startNewGame() {
        currentScore = 0
        askedQuestionIds.clear()
        gameActive = true

        scoreLayout.visibility = View.GONE
        llChallengeContainer.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        tvInstructions.visibility = View.VISIBLE

        startTimer()
        nextQuestion()
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(totalTimeMillis, intervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvTimer.text = "Timer: $secondsLeft"
            }

            override fun onFinish() {
                gameActive = false
                tvTimer.text = "Time's up!"
                showFinalScore()
            }
        }
        countDownTimer.start()
    }

    private fun nextQuestion() {
        if (!gameActive) return

        val available = allChallenges.filter { it.id !in askedQuestionIds }
        if (available.isEmpty()) {
            askedQuestionIds.clear()
        }

        currentChallenge = allChallenges.filter { it.id !in askedQuestionIds }.randomOrNull() ?: allChallenges.random()
        currentChallenge?.let { challenge ->
            askedQuestionIds.add(challenge.id)
            displayChallenge(challenge)
            tvFeedback.visibility = View.GONE
            tvScore.text = getString(R.string.score_placeholder, currentScore)
        }
    }

    private fun displayChallenge(challenge: CodeChallenge) {
        tvChallengeDescription.text = challenge.description
        tvBuggyCode.text = android.text.Html.fromHtml(challenge.buggyCode, android.text.Html.FROM_HTML_MODE_LEGACY)
        addChallengeOptions(challenge.options, challenge.correctFix)
    }

    private fun addChallengeOptions(options: List<String>, correctFix: String) {
        llChallengeOptions.removeAllViews()
        options.shuffled().forEach { optionText ->
            val btn = Button(this).apply {
                text = android.text.Html.fromHtml(optionText, android.text.Html.FROM_HTML_MODE_LEGACY)
                textSize = 16f
                setTextColor(ResourcesCompat.getColor(resources, R.color.text_light, theme))
                setBackgroundResource(R.drawable.rounded_button_background)
                setPadding(20, 20, 20, 20)
                isAllCaps = false
                setOnClickListener {
                    playClickSound()
                    val isCorrect = optionText == correctFix
                    if (isCorrect) {
                        currentScore++
                        playCorrectSound()
                        tvFeedback.text = getString(R.string.correct_answer)
                        tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.correct_green, theme))
                    } else {
                        playWrongSound()
                        tvFeedback.text = getString(R.string.wrong_answer)
                        tvFeedback.setTextColor(ResourcesCompat.getColor(resources, R.color.error_red, theme))
                    }
                    tvFeedback.visibility = View.VISIBLE
                    disableOptions()
                    tvInstructions.postDelayed({
                        if (gameActive) {
                            nextQuestion()
                        }
                    }, 1200)
                }
            }
            llChallengeOptions.addView(btn)
        }
    }

    private fun disableOptions() {
        for (btn in llChallengeOptions.children) {
            btn.isEnabled = false
        }
    }

    private fun showFinalScore() {
        llChallengeContainer.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = getString(R.string.final_score_message, currentScore)
        btnStartGame.visibility = View.GONE

        // ---- PROGRESS UPDATE LOGIC ----
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.get().addOnSuccessListener { document ->
                val prevGames = document.getLong("totalGamesPlayed") ?: 0
                val prevScore = document.getLong("bestScore") ?: 0
                val newBest = if (currentScore > prevScore) currentScore else prevScore

                userRef.update(
                    mapOf(
                        "totalGamesPlayed" to prevGames + 1,
                        "bestScore" to newBest
                    )
                )
            }
        }
    }

    private fun playClickSound() {
        if (mediaPlayerClick.isPlaying) mediaPlayerClick.seekTo(0)
        mediaPlayerClick.start()
    }

    private fun playCorrectSound() {
        if (mediaPlayerCorrect.isPlaying) mediaPlayerCorrect.seekTo(0)
        mediaPlayerCorrect.start()
    }

    private fun playWrongSound() {
        if (mediaPlayerWrong.isPlaying) mediaPlayerWrong.seekTo(0)
        mediaPlayerWrong.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) countDownTimer.cancel()
        mediaPlayerClick.release()
        mediaPlayerCorrect.release()
        mediaPlayerWrong.release()
    }
}
