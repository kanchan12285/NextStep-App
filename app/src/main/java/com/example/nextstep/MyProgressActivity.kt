package com.example.nextstep

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nextstep.ui.setOnClickWithSound

class MyProgressActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivBackArrow: ImageView
    private lateinit var tvTotalGamesPlayed: TextView
    private lateinit var tvBestScore: TextView

    private val TAG = "MyProgressActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_progress)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivBackArrow = findViewById(R.id.ivBackArrow)
        tvTotalGamesPlayed = findViewById(R.id.tvTotalGamesPlayed)
        tvBestScore = findViewById(R.id.tvBestScore)

        ivBackArrow.setOnClickWithSound {
            onBackPressedDispatcher.onBackPressed()
        }

        loadUserStats()
    }

    private fun loadUserStats() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to view your progress.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalGames = document.getLong("totalGamesPlayed") ?: 0
                    val bestScore = document.getLong("bestScore") ?: 0

                    tvTotalGamesPlayed.text = totalGames.toString()
                    tvBestScore.text = bestScore.toString()
                } else {
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user stats: ${e.message}", e)
                Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show()
            }
    }
}
