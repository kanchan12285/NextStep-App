package com.example.nextstep

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import com.example.nextstep.ui.QuizSoundHelper
import com.example.nextstep.ui.setOnClickWithSound
import android.media.MediaPlayer
import kotlin.random.Random

class ResourceBalancerActivity : BaseActivity() {

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
    private var currentChallengeView: View? = null
    private val askedQuestions = mutableSetOf<Int>()

    private val GAME_DURATION_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())

    private val allVariants = listOf(
        ChallengeData(
            "Distribute limited resources to tasks",
            "Available Resources:\n• Engineers: 3\n• Designers: 2\n• QA: 2",
            listOf(
                TaskData("Task A (High): Engineers 2, QA 1"),
                TaskData("Task B (Medium): Engineers 1, Designers 1"),
                TaskData("Task C (Low): Designers 1")
            )
        )
    )

    private val userAllocations = mutableMapOf<Int, ResourceAllocation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource_balancer)

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
        val inflater = layoutInflater
        val scoreView = inflater.inflate(R.layout.layout_score_display, rootView, false)
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

        btnStartGame.setOnClickWithSound { startGame() }
        btnBackToDashboard.setOnClickWithSound {
            playClickSound()
            finish()
        }

        resetGameUI()
    }

    private fun resetGameUI() {
        tvTimer.text = "Time: ${GAME_DURATION_MS / 1000}s"
        tvScore.text = "Score: 0"
        tvInstructions.text = "Allocate resources strategically to complete tasks efficiently. Consider priorities and constraints."
        tvInstructions.visibility = View.VISIBLE
        tvFeedback.visibility = View.GONE
        btnStartGame.text = "Start Game"
        btnStartGame.visibility = View.VISIBLE
        btnBackToDashboard.visibility = View.VISIBLE
        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.GONE
        scoreLayout.visibility = View.GONE

        score = 0
        gameRunning = false
        currentChallengeView = null
        askedQuestions.clear()
        userAllocations.clear()
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
        score = 0
        gameRunning = true
        askedQuestions.clear()
        userAllocations.clear()

        tvScore.text = "Score: 0"
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE
        scoreLayout.visibility = View.GONE

        llChallengeContainer.removeAllViews()
        llChallengeContainer.visibility = View.VISIBLE

        startTimer()
        displayChallenge()
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

    private fun displayChallenge() {
        if (!gameRunning) return

        currentChallengeView?.let { llChallengeContainer.removeView(it) }

        val availableIndices = allVariants.indices.filter { it !in askedQuestions }
        val indicesList = if (availableIndices.isEmpty()) {
            askedQuestions.clear()
            allVariants.indices.toList()
        } else {
            availableIndices
        }
        val selectedIndex = indicesList.random()
        askedQuestions.add(selectedIndex)

        val variant = allVariants[selectedIndex]
        userAllocations.clear()

        val view = tryInflateChildLayout(variant, selectedIndex)
        llChallengeContainer.addView(view)
        currentChallengeView = view
    }

    private fun tryInflateChildLayout(data: ChallengeData, challengeIndex: Int): View {
        val v = LayoutInflater.from(this)
            .inflate(R.layout.item_resource_balancer_puzzle, llChallengeContainer, false)

        val tvTitle: TextView? = v.findViewById(resources.getIdentifier("tvBalanceScenario", "id", packageName))
        val tvAvail: TextView? = v.findViewById(resources.getIdentifier("tvAvailableResources", "id", packageName))
        val llTasks: LinearLayout? = v.findViewById(resources.getIdentifier("llResourceSliders", "id", packageName))
        val btnSubmit: Button? = v.findViewById(resources.getIdentifier("btnCheckBalance", "id", packageName))

        require(tvTitle != null && tvAvail != null && llTasks != null && btnSubmit != null) {
            "Required views are missing from layout"
        }

        tvTitle.text = data.title
        tvAvail.text = data.availableResources
        llTasks.removeAllViews()

        data.tasks.forEachIndexed { index, task ->
            createTaskAllocationView(llTasks, task, challengeIndex, index)
        }

        btnSubmit.setOnClickWithSound {
            handleSubmit(challengeIndex, data)
        }

        return v
    }

    private fun createTaskAllocationView(
        parent: LinearLayout,
        taskData: TaskData,
        challengeIndex: Int,
        taskIndex: Int
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
        }
        val tvTask = TextView(this).apply {
            text = taskData.description
            textSize = 16f
        }
        container.addView(tvTask)

        userAllocations[taskIndex] = ResourceAllocation()

        container.addView(createResourceSlider("Engineers", 3) { progress ->
            userAllocations[taskIndex]?.engineers = progress
        })

        container.addView(createResourceSlider("Designers", 2) { progress ->
            userAllocations[taskIndex]?.designers = progress
        })

        container.addView(createResourceSlider("QA", 2) { progress ->
            userAllocations[taskIndex]?.qa = progress
        })

        parent.addView(container)
    }

    private fun createResourceSlider(label: String, max: Int, onProgressChanged: (Int) -> Unit): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = 8.dpToPx()
            setPadding(padding, padding / 2, padding, padding / 2)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val tvLabel = TextView(this).apply {
            text = "$label: 0"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        val seekBar = SeekBar(this).apply {
            this.max = max
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 5f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvLabel.text = "$label: $progress"
                    onProgressChanged(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(tvLabel)
        layout.addView(seekBar)
        return layout
    }

    private fun handleSubmit(challengeIndex: Int, data: ChallengeData) {
        if (!gameRunning) return

        val totalEngineers = userAllocations.values.sumOf { it.engineers }
        val totalDesigners = userAllocations.values.sumOf { it.designers }
        val totalQA = userAllocations.values.sumOf { it.qa }

        val maxEngineers = 3
        val maxDesigners = 2
        val maxQA = 2

        if (totalEngineers > maxEngineers || totalDesigners > maxDesigners || totalQA > maxQA) {
            tvFeedback.text = "You assigned more resources than available! Check your allocations."
            tvFeedback.setTextColor(resources.getColor(R.color.error_red, theme))
            tvFeedback.visibility = View.VISIBLE
            playWrongSound()
            return
        }

        score += 1
        tvScore.text = "Score: $score"
        tvFeedback.text = "Good strategic thinking! +1 point."
        tvFeedback.setTextColor(resources.getColor(R.color.success_green, theme))
        tvFeedback.visibility = View.VISIBLE
        playCorrectSound()

        disableCurrentInputs()

        handler.postDelayed({
            tvFeedback.visibility = View.GONE
            displayChallenge()
        }, 1500)
    }

    private fun disableCurrentInputs() {
        currentChallengeView?.findViewById<LinearLayout>(resources.getIdentifier("llResourceSliders", "id", packageName))
            ?.children?.forEach { view ->
                if (view is LinearLayout) {
                    view.children.forEach { childView -> childView.isEnabled = false }
                }
            }

        currentChallengeView?.findViewById<Button>(resources.getIdentifier("btnCheckBalance", "id", packageName))
            ?.isEnabled = false
    }

    private fun endGame() {
        gameRunning = false
        countDownTimer?.cancel()
        currentChallengeView?.let { llChallengeContainer.removeView(it) }
        llChallengeContainer.visibility = View.GONE
        tvInstructions.visibility = View.GONE
        tvFeedback.visibility = View.GONE
        btnStartGame.visibility = View.GONE
        btnBackToDashboard.visibility = View.GONE

        tvFinalScore.text = getString(R.string.final_score_message, score)
        scoreLayout.visibility = View.VISIBLE
        updateUserProgress(score)

    }

    private fun Int.dpToPx(): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()

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

    data class ChallengeData(
        val title: String,
        val availableResources: String,
        val tasks: List<TaskData>
    )

    data class TaskData(
        val description: String
    )

    data class ResourceAllocation(
        var engineers: Int = 0,
        var designers: Int = 0,
        var qa: Int = 0
    )
}
