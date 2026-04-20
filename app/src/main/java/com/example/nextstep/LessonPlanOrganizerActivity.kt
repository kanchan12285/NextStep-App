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

class LessonPlanOrganizerActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnStartGame: Button
    private lateinit var btnBackToDashboard: Button
    private lateinit var llChallengeContainer: LinearLayout

    // Final score layout views (dynamically inflated)
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

    private var lessonPlanChallenges: List<LessonPlanChallenge> = emptyList()
    private var currentChallenge: LessonPlanChallenge? = null
    private var currentChallengeView: View? = null
    private val askedIndices = mutableSetOf<Int>()
    private val selectedOrder: MutableList<String> = mutableListOf()

    private val GAME_DURATION_MS = 30_000L
    private val TAG = "LessonPlanOrganizerActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_plan_organizer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartGame = findViewById(R.id.btnStartGame)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
        llChallengeContainer = findViewById(R.id.llChallengeContainer)

        mediaPlayerClick = MediaPlayer.create(this, R.raw.button_click)
        mediaPlayerCorrect = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_answer)

        // Inflate common final score layout dynamically
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
        rootView.addView(scoreView)
        scoreLayout = scoreView.findViewById(R.id.scoreLayout)
        tvFinalScore = scoreView.findViewById(R.id.tvFinalScore)
        btnPlayAgain = scoreView.findViewById(R.id.btnPlayAgain)
        btnExit = scoreView.findViewById(R.id.btnExit)
        scoreLayout.visibility = View.GONE

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

        loadLessonPlanChallenges()
        resetGameUI()
    }

    private fun loadLessonPlanChallenges() {
        try {
            val jsonString = assets.open("lesson_plan_organizer_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<LessonPlanChallenge>>() {}.type
            lessonPlanChallenges = Gson().fromJson(jsonString, type)
            if (lessonPlanChallenges.isEmpty()) {
                Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_LONG).show()
                Log.e(TAG, "lesson_plan_organizer_data.json is empty or could not be parsed.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading lesson plan challenges: ${e.message}")
            Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_LONG).show()
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
        tvTimer.text = getString(R.string.timer_label, GAME_DURATION_MS / 1000)
        score = 0
        tvScore.text = getString(R.string.score_label, 0)
        gameActive = false
        selectedOrder.clear()
        askedIndices.clear()
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
        if (lessonPlanChallenges.isEmpty()) {
            Toast.makeText(this, getString(R.string.data_not_loaded), Toast.LENGTH_SHORT).show()
            return
        }

        score = 0
        gameActive = true
        tvScore.text = getString(R.string.score_label, score)
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
                tvTimer.text = getString(R.string.timer_label, millisUntilFinished / 1000)
            }

            override fun onFinish() {
                tvTimer.text = getString(R.string.timer_label, 0)
                endGame()
            }
        }.start()
    }

    private fun displayNextChallenge() {
        if (!gameActive) return

        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        selectedOrder.clear()

        val availableIndices = lessonPlanChallenges.indices.filter { it !in askedIndices }
        if (availableIndices.isEmpty()) {
            endGame()
            return
        }
        val selectedIndex = availableIndices.random()
        askedIndices.add(selectedIndex)
        currentChallenge = lessonPlanChallenges[selectedIndex]

        currentChallenge?.let { challenge ->
            val challengeLayout = LayoutInflater.from(this).inflate(R.layout.item_lesson_plan_challenge, llChallengeContainer, false)

            val tvTopic: TextView = challengeLayout.findViewById(R.id.tvTopic)
            val tvObjectives: TextView = challengeLayout.findViewById(R.id.tvObjectives)
            val llActivitiesContainer: LinearLayout = challengeLayout.findViewById(R.id.llActivitiesContainer)
            val btnSubmitOrder: Button = challengeLayout.findViewById(R.id.btnSubmitOrder)

            tvTopic.text = "Topic: ${challenge.topic}"
            tvObjectives.text = "Objectives: ${challenge.objectives}"

            llActivitiesContainer.removeAllViews()

            challenge.activities.shuffled().forEach { activity ->
                val activityButton = Button(this).apply {
                    text = "${activity.name} (${activity.durationMinutes} mins)"
                    setBackgroundResource(R.drawable.rounded_button_background)
                    setTextColor(ResourcesCompat.getColor(resources, R.color.button_text_light, theme))
                    isAllCaps = false
                    setOnClickListener {
                        if (selectedOrder.contains(activity.name)) {
                            selectedOrder.remove(activity.name)
                            setBackgroundResource(R.drawable.rounded_button_background)
                        } else {
                            selectedOrder.add(activity.name)
                            setBackgroundResource(R.drawable.rounded_button_selected_background)
                        }
                    }
                }
                llActivitiesContainer.addView(activityButton)
            }

            btnSubmitOrder.isEnabled = true
            btnSubmitOrder.setOnClickListener {
                handleSubmitOrder(challenge, btnSubmitOrder)
            }

            llChallengeContainer.addView(challengeLayout)
            currentChallengeView = challengeLayout
        } ?: endGame()
    }

    private fun handleSubmitOrder(challenge: LessonPlanChallenge, btnSubmitOrder: Button) {
        if (!gameActive) return

        val isCorrect = selectedOrder == challenge.correctOrder
        if (isCorrect) {
            score += 1
            tvScore.text = getString(R.string.score_label, score)
            tvFeedback.text = getString(R.string.correct_plus_10).replace("+10", "+1")
            playCorrectSound()
        } else {
            tvFeedback.text = getString(R.string.feedback_incorrect_lesson_plan_organizer, challenge.correctOrder.joinToString(" -> "))
            playWrongSound()
        }

        tvFeedback.visibility = View.VISIBLE
        btnSubmitOrder.isEnabled = false

        handler.postDelayed({
            tvFeedback.visibility = View.GONE
            displayNextChallenge()
        }, 2000)

        updateUserScoreInFirestore(if (isCorrect) 1 else 0)
    }

    private fun updateUserScoreInFirestore(points: Int) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("personalityScores.organization", FieldValue.increment(points.toLong()))
            .addOnSuccessListener { Log.d(TAG, "Organization score updated by $points") }
            .addOnFailureListener { e -> Log.e(TAG, "Error updating score: ${e.message}") }
    }

    private fun endGame() {
        gameActive = false
        countDownTimer?.cancel()
        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        llChallengeContainer.visibility = View.GONE

        // Show final score layout here and hide gameplay UI properly
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE

        // Display final score in the dynamically inflated score layout
        tvFinalScore.text = getString(R.string.final_score_message, score)
        scoreLayout.visibility = View.VISIBLE
        updateUserProgress(score)

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
