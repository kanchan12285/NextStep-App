package com.example.nextstep

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nextstep.ui.setOnClickWithSound

class PersonalityResultActivity : AppCompatActivity() {

    private lateinit var tvPersonalityResult: TextView
    private lateinit var tvResultDescription: TextView
    private lateinit var btnViewPersonalityCard: Button
    private lateinit var btnRetakeQuiz: Button
    private lateinit var btnBackToHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personality_result)

        // Initialize views
        tvPersonalityResult = findViewById(R.id.tvPersonalityResult)
        tvResultDescription = findViewById(R.id.tvResultDescription)
        btnViewPersonalityCard = findViewById(R.id.btnViewPersonalityCard)
        btnRetakeQuiz = findViewById(R.id.btnRetakeQuiz)
        btnBackToHome = findViewById(R.id.btnBackToHome)

        // Get data from intent (e.g., result type & description)
        val personalityType = intent.getStringExtra("PERSONALITY_TYPE") ?: "Explorer"
        val personalityDescription = intent.getStringExtra("PERSONALITY_DESCRIPTION") ?: "Your personality details will appear here."

        tvPersonalityResult.text = personalityType
        tvResultDescription.text = personalityDescription

        // Button actions
        btnViewPersonalityCard.setOnClickWithSound {
            val intent = Intent(this, PersonalityCardActivity::class.java) // Replace with your quiz activity
            startActivity(intent)
            finish()
        }


        btnRetakeQuiz.setOnClickWithSound {
            val intent = Intent(this, PersonalityResultActivity::class.java) // Replace with your quiz activity
            startActivity(intent)
            finish()
        }

        btnBackToHome.setOnClickWithSound {
            val intent = Intent(this, HomeDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
