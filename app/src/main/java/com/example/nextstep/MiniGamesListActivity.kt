package com.example.nextstep

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import com.example.nextstep.ui.setOnClickWithSound

class MiniGamesListActivity : BaseActivity() {

    private lateinit var rvMiniGames: RecyclerView
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvMiniGamesListPrompt: TextView
    private var miniGameConfigs: List<MiniGameConfig> = emptyList()

    private val TAG = "MiniGamesListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mini_games_list)

        try {
            Log.d(TAG, "🚀 Starting MiniGamesListActivity")
            initializeViews()
            setupClickListeners()
            setupRecyclerView()
            loadMiniGameConfigs()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading mini-games", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        rvMiniGames = findViewById(R.id.rvMiniGames)
        ivBackArrow = findViewById(R.id.ivBackArrow)
        tvMiniGamesListPrompt = findViewById(R.id.tvMiniGamesListPrompt)
        Log.d(TAG, "✅ Views initialized successfully")
    }

    private fun setupClickListeners() {
        // Back arrow with sound
        ivBackArrow.setOnClickWithSound {
            Log.d(TAG, "🔙 Back arrow clicked")
            onBackPressedDispatcher.onBackPressed()
        }
        Log.d(TAG, "✅ Click listeners setup successfully")
    }

    private fun setupRecyclerView() {
        rvMiniGames.setHasFixedSize(true)
        rvMiniGames.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvMiniGames.addItemDecoration(
            VerticalSpaceDecoration(resources.getDimensionPixelSize(R.dimen.list_item_spacing))
        )
        Log.d(TAG, "✅ RecyclerView setup with LinearLayoutManager (vertical)")
    }

    private fun loadMiniGameConfigs() {
        Log.d(TAG, "📂 Loading mini-game configurations...")

        try {
            val jsonString = assets.open("mini_games_config.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<MiniGameConfig>>() {}.type
            miniGameConfigs = Gson().fromJson(jsonString, type)

            // Exclude the two games not needed
            val excluded = setOf("resource_allocator", "pattern_finder")
            miniGameConfigs = miniGameConfigs.filter { it.id !in excluded }

            Log.d(TAG, "✅ Parsed ${miniGameConfigs.size} mini-game configurations (filtered)")

            if (miniGameConfigs.isNotEmpty()) {
                setupAdapter()
            } else {
                Log.w(TAG, "⚠️ No mini-game configurations after filtering")
                showNoGamesError()
            }

        } catch (e: IOException) {
            Log.e(TAG, "❌ Error loading mini_games_config.json: ${e.message}", e)
            Toast.makeText(this, "Error: mini_games_config.json not found or cannot be read.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing mini_games_config.json: ${e.message}", e)
            Toast.makeText(this, "Error: Invalid JSON format in mini_games_config.json", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAdapter() {
        val adapter = MiniGamesAdapter(miniGameConfigs) { config ->
            launchMiniGame(config)
        }
        rvMiniGames.adapter = adapter
        Log.d(TAG, "✅ Adapter set successfully with ${miniGameConfigs.size} games")
    }

    private fun launchMiniGame(config: MiniGameConfig) {
        try {
            val intent = Intent(this, MiniGameActivity::class.java).putExtra("GAME_ID", config.id)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error launching ${config.name}: ${e.message}", e)
            Toast.makeText(this, "Error launching ${config.name}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showNoGamesError() {
        tvMiniGamesListPrompt.text = "No mini-games available. Please check the configuration file."
        Toast.makeText(this, "No mini-games found. Check mini_games_config.json", Toast.LENGTH_LONG).show()
    }
}

/** Simple vertical spacing for list rows. */
class VerticalSpaceDecoration(private val spacePx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val pos = parent.getChildAdapterPosition(view)
        if (pos == RecyclerView.NO_POSITION) return
        outRect.top = if (pos == 0) spacePx else 0
        outRect.bottom = spacePx
    }
}
