package com.example.nextstep

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlin.random.Random

class MultiplayerActivity : BaseActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var tvModeTitle: TextView
    private lateinit var btnCreateRoom: Button
    private lateinit var btnJoinRoom: Button
    private lateinit var etRoomCode: EditText
    private lateinit var rvPlayersList: RecyclerView
    private lateinit var btnStartMultiplayerGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer)

        mediaPlayer = MediaPlayer.create(this, R.raw.button_click)

        tvModeTitle = findViewById(R.id.tvModeTitle)
        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        btnJoinRoom = findViewById(R.id.btnJoinRoom)
        etRoomCode = findViewById(R.id.etRoomCode)
        rvPlayersList = findViewById<RecyclerView>(R.id.rvPlayersList)
        btnStartMultiplayerGame = findViewById(R.id.btnStartMultiplayerGame)

        // Back arrow uses the system back dispatcher (predictive-back compatible)
        findViewById<ImageView>(R.id.ivBackArrow).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnCreateRoom.setOnClickListener { playClickSound(); createRoom() }
        btnJoinRoom.setOnClickListener { playClickSound(); joinRoom() }
        btnStartMultiplayerGame.setOnClickListener { playClickSound(); startGame() }
    }

    private fun playClickSound() {
        if (mediaPlayer.isPlaying) mediaPlayer.seekTo(0)
        mediaPlayer.start()
    }

    private fun createRoom() {
        val roomCode = generateRoomCode()
        AlertDialog.Builder(this)
            .setTitle("🏠 Room Created!")
            .setMessage("Your Room Code: $roomCode\n\nShare this code with friends to join.")
            .setPositiveButton("Start Game") { _, _ -> startGame() }
            .setNeutralButton("Copy Code") { _, _ ->
                copyToClipboard(roomCode)
                Toast.makeText(this, "📋 Room code copied!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinRoom() {
        val roomCode = etRoomCode.text.toString().trim().uppercase()
        if (roomCode.length == 6) {
            Toast.makeText(this, "🎮 Joining room $roomCode...", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                if (Random.nextBoolean()) {
                    Toast.makeText(this, "✅ Successfully joined room $roomCode!", Toast.LENGTH_SHORT).show()
                    startGame()
                } else {
                    Toast.makeText(this, "❌ Room $roomCode not found or full", Toast.LENGTH_SHORT).show()
                }
            }, 2000)
        } else {
            Toast.makeText(this, "❌ Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGame() {
        startActivity(Intent(this, MiniGamesListActivity::class.java))
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Room Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
