package com.example.nextstep

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.core.view.children
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class ColorPaletteMixerActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var gameActive = false

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContent: LinearLayout
    private lateinit var tvDescription: TextView
    private lateinit var llBaseColorsContainer: LinearLayout
    private lateinit var llOptionsContainer: LinearLayout

    // Final score layout views (inflated dynamically)
    private lateinit var scoreLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnExit: Button

    private lateinit var mediaPlayerClick: MediaPlayer
    private lateinit var mediaPlayerCorrect: MediaPlayer
    private lateinit var mediaPlayerWrong: MediaPlayer

    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private var colorPaletteChallenges: List<ColorPaletteChallenge> = emptyList()
    private var currentChallenge: ColorPaletteChallenge? = null

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "ColorPaletteMixerActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_palette_mixer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize main UI views
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContent = findViewById(R.id.llChallengeContent)
        tvDescription = findViewById(R.id.tvDescription)
        llBaseColorsContainer = findViewById(R.id.llBaseColorsContainer)
        llOptionsContainer = findViewById(R.id.llOptionsContainer)

        // Prepare media players for sounds
        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

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

        // Button listeners with sound integration
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
            updateUIForStart()
        }
        btnExit.setOnClickListener {
            playClickSound()
            finish()
        }

        loadColorPaletteChallenges()
        updateUIForStart()
    }

    private fun updateUIForStart() {
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
    }

    private fun loadColorPaletteChallenges() {
        try {
            val jsonString = assets.open("color_palette_mixer_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<ColorPaletteChallenge>>() {}.type
            colorPaletteChallenges = Gson().fromJson(jsonString, type)
            if (colorPaletteChallenges.isEmpty()) {
                Toast.makeText(this, "No color palette challenges found. Please check color_palette_mixer_data.json", Toast.LENGTH_LONG).show()
                Log.e(TAG, "color_palette_mixer_data.json is empty or could not be parsed.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading color palette challenges from assets: ${e.message}")
            Toast.makeText(this, "Error loading game data.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing color palette challenges JSON: ${e.message}")
            Toast.makeText(this, "Error parsing game data.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startGame() {
        if (colorPaletteChallenges.isEmpty()) {
            Toast.makeText(this, "Game data not loaded. Cannot start game.", Toast.LENGTH_SHORT).show()
            return
        }

        score = 0
        gameActive = true
        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        llChallengeContent.visibility = View.VISIBLE

        startTimer()
        displayRandomChallenge()
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
        if (!gameActive || colorPaletteChallenges.isEmpty()) return

        // Re-enable options before showing new challenge
        enableOptions()

        currentChallenge = colorPaletteChallenges.shuffled().firstOrNull()

        currentChallenge?.let { challenge ->
            tvDescription.text = challenge.description

            llBaseColorsContainer.removeAllViews()
            challenge.baseColors.forEach { colorHex ->
                val colorView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        60.dpToPx(),
                        60.dpToPx()
                    ).apply { setMargins(8.dpToPx(), 0, 8.dpToPx(), 0) }
                    try {
                        setBackgroundColor(Color.parseColor(colorHex))
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Invalid color hex: $colorHex")
                        setBackgroundColor(Color.GRAY)
                    }
                }
                llBaseColorsContainer.addView(colorView)
            }

            llOptionsContainer.removeAllViews()
            challenge.options.shuffled().forEachIndexed { index, palette ->
                val optionButton = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 8, 0, 8) }
                    text = "Option ${index + 1}: ${palette.joinToString(", ")}"
                    setBackgroundResource(R.drawable.rounded_button_background)
                    setTextColor(ResourcesCompat.getColor(resources, R.color.button_text_light, theme))

                    setOnClickListener {
                        playClickSound()
                        handleAnswer(palette)
                    }
                }
                llOptionsContainer.addView(optionButton)

                val paletteVisualContainer = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 4, 0, 12) }
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                }

                palette.forEach { colorHex ->
                    val colorSwatch = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                            setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
                        }
                        try {
                            setBackgroundColor(Color.parseColor(colorHex))
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid color hex in option: $colorHex")
                            setBackgroundColor(Color.GRAY)
                        }
                    }
                    paletteVisualContainer.addView(colorSwatch)
                }
                llOptionsContainer.addView(paletteVisualContainer)
            }
        } ?: endGame()
    }

    private fun handleAnswer(selectedPalette: List<String>) {
        if (!gameActive) return

        currentChallenge?.let { challenge ->
            val isCorrect = selectedPalette == challenge.correctPalette
            if (isCorrect) {
                score++
                tvScore.text = "Score: $score"
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
                    displayRandomChallenge()
                }
            }, 1200)
        }
    }

    private fun enableOptions() {
        for (btn in llOptionsContainer.children) {
            btn.isEnabled = true
        }
    }

    private fun disableOptions() {
        for (btn in llOptionsContainer.children) {
            btn.isEnabled = false
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

    private fun endGame() {
        gameActive = false
        countDownTimer?.cancel()
        llChallengeContent.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE

        scoreLayout.visibility = View.VISIBLE
        tvFinalScore.text = getString(R.string.final_score_message, score)

        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE

        // --- Firestore update for progress tracking ---
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.get().addOnSuccessListener { document ->
                val previousGames = document.getLong("totalGamesPlayed") ?: 0
                val previousBest = document.getLong("bestScore") ?: 0
                val newBest = if (score > previousBest) score else previousBest
                userRef.update(
                    mapOf(
                        "totalGamesPlayed" to previousGames + 1,
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

    private fun Int.dpToPx(): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        resources.displayMetrics
    ).toInt()

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        mediaPlayerClick.release()
        mediaPlayerCorrect.release()
        mediaPlayerWrong.release()
    }
}
