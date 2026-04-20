package com.example.nextstep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nextstep.ui.setOnClickWithSound

class HelpFaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_faq)

        val btnBackToHome = findViewById<Button>(R.id.btnBackToHome)
        val btnContactUs = findViewById<Button>(R.id.btnContactUs)
        val llFaqContainer = findViewById<LinearLayout>(R.id.llFaqContainer)

        setupFaqItems(llFaqContainer)

        btnBackToHome.setOnClickWithSound {
            finish()
        }

        btnContactUs.setOnClickWithSound {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@nextstepgaming.com")
                putExtra(Intent.EXTRA_SUBJECT, "NextStep Gaming - Support Request")
                putExtra(Intent.EXTRA_TEXT, "Hello NextStep Team,\n\nI need help with:\n\n")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }
    }

    private fun setupFaqItems(container: LinearLayout) {
        val faqItems = listOf(
            "🎮 How do I play mini-games?" to "Simply tap on 'MINI GAMES' from the home screen and choose any game you'd like to play. Each game has instructions before you start.",
            "🤝 How does 'Play with Friends' work?" to "Create a room and share the 6-digit code with your friends. They can enter the code to join your room and play together.",
            "🌍 What is 'Play Online'?" to "This feature matches you with random players online for competitive gaming. Just tap the button and we'll find opponents for you.",
            "📋 What is the 'Know Me Card'?" to "Your personality card shows your gaming strengths and personality type based on how you play different mini-games.",
            "💙 What is C.A.R.E for You?" to "C.A.R.E provides mental health resources, wellness tips, and support for your overall well-being.",
            "🏆 How do I level up?" to "Play more mini-games to increase your level. Each game you complete adds to your experience and unlocks new features.",
            "🎯 How are personality traits calculated?" to "Your traits are analyzed based on your performance in different types of games - logic games boost logic, creative games boost creativity, etc.",
            "🔧 App not working properly?" to "Try restarting the app. If problems persist, use the 'Report Issues' feature to let us know what's happening."
        )

        faqItems.forEach { (question, answer) ->
            addFaqItem(container, question, answer)
        }
    }

    private fun addFaqItem(container: LinearLayout, question: String, answer: String) {
        val faqLayout = LayoutInflater.from(this).inflate(R.layout.faq_item, container, false) as LinearLayout
        val tvQuestion = faqLayout.findViewById<TextView>(R.id.tvQuestion)
        val tvAnswer = faqLayout.findViewById<TextView>(R.id.tvAnswer)

        tvQuestion.text = question
        tvAnswer.text = answer

        faqLayout.setOnClickListener {
            tvAnswer.visibility = if (tvAnswer.visibility == android.view.View.VISIBLE) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }

        container.addView(faqLayout)
    }
}
