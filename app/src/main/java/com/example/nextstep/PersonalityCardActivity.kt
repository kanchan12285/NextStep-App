package com.example.nextstep

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class PersonalityCardActivity : BaseActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var imgAvatar: ImageView
    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerAge: TextView
    private lateinit var tvPersonalityType: TextView
    private lateinit var tvPersonalityDescription: TextView
    private lateinit var traitsContainer: LinearLayout
    private lateinit var cardContainer: androidx.cardview.widget.CardView
    private lateinit var btnDownload: Button
    private lateinit var btnShare: Button

    private val scores = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personality_card)

        imgAvatar = findViewById(R.id.imgAvatar)
        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerAge = findViewById(R.id.tvPlayerAge)
        tvPersonalityType = findViewById(R.id.tvPersonalityType)
        tvPersonalityDescription = findViewById(R.id.tvPersonalityDescription)
        traitsContainer = findViewById(R.id.traitsContainer)
        cardContainer = findViewById(R.id.cardContainer)
        btnDownload = findViewById(R.id.btnDownloadCard)
        btnShare = findViewById(R.id.btnShareCard)

        btnDownload.setOnClickListener { saveCardToGallery() }
        btnShare.setOnClickListener { shareCardImage() }

        bindHeader()
        loadScores()
    }

    private fun bindHeader() {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid == null) {
            Log.w("PersonalityCard", "User not authenticated")
            setDefaultUserInfo()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = when {
                    !user?.displayName.isNullOrBlank() -> user.displayName!!
                    !doc.getString("fullName").isNullOrBlank() -> doc.getString("fullName")!!
                    else -> "Player"
                }
                val age = doc.getLong("age")?.toInt() ?: 21
                val avatarKey = doc.getString("avatar") ?: "avatar_1"

                val avatarResId = when (avatarKey) {
                    "avatar_1" -> R.drawable.avatar_1
                    "avatar_2" -> R.drawable.avatar_2
                    "avatar_3" -> R.drawable.avatar_3
                    "avatar_4" -> R.drawable.avatar_4
                    "avatar_5" -> R.drawable.avatar_5
                    else -> R.drawable.avatar_1
                }

                tvPlayerName.text = name
                tvPlayerAge.text = "Age: $age"
                imgAvatar.setImageResource(avatarResId)

                Log.d("PersonalityCard", "Name: $name, Avatar Key: $avatarKey, Age: $age")
            }
            .addOnFailureListener {
                Log.e("PersonalityCard", "Failed to load profile data")
                setDefaultUserInfo()
            }
    }

    private fun setDefaultUserInfo() {
        tvPlayerName.text = "Player"
        tvPlayerAge.text = "Age: --"
        imgAvatar.setImageResource(R.drawable.avatar_placeholder)
    }

    private fun loadScores() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                scores.clear()
                @Suppress("UNCHECKED_CAST")
                val map = doc.get("personalityScores") as? Map<String, Long>
                map?.forEach { (k, v) -> scores[k] = v.toInt() }
                ensureAllTraitsPresent()
                bindType()
                bindTraits()
            }
            .addOnFailureListener {
                Log.e("PersonalityCard", "Failed to load personality scores")
            }
    }

    // Ensure all known traits have at least 0 score for display consistency
    private fun ensureAllTraitsPresent() {
        val allTraitsKeys = listOf(
            "analytical_thinking",
            "creativity",
            "empathy",
            "leadership",
            "adaptability",
            "discipline",
            "optimism",
            "curiosity",
            "resilience",
            "collaboration"
        )
        allTraitsKeys.forEach { key ->
            if (!scores.containsKey(key)) {
                scores[key] = 0
            }
        }
    }

    private fun bindType() {
        if (scores.isEmpty()) return

        val traitTitles = mapOf(
            "analytical_thinking" to ("The Logical Thinker" to "You excel at problem-solving and analytical thinking."),
            "creativity" to ("The Creative Mind" to "You come up with innovative ideas."),
            "empathy" to ("The Empath" to "You connect easily with others."),
            "leadership" to ("The Leader" to "You guide and inspire people."),
            "adaptability" to ("The Flexible Navigator" to "You easily adjust to changing situations."),
            "discipline" to ("The Consistent Achiever" to "You stay focused and accomplish goals."),
            "optimism" to ("The Positive Visionary" to "You inspire hope and confidence."),
            "curiosity" to ("The Inquisitive Explorer" to "You love learning and discovery."),
            "resilience" to ("The Persistent Fighter" to "You bounce back strongly from setbacks."),
            "collaboration" to ("The Team Player" to "You work well with others to reach common goals.")
        )

        val topTraitKey = scores.maxByOrNull { it.value }?.key?.lowercase(Locale.getDefault())

        val (title, description) = if (topTraitKey != null && traitTitles.containsKey(topTraitKey)) {
            traitTitles[topTraitKey]!!
        } else {
            // If for some reason there is no top trait, assign the first trait title as default
            traitTitles.values.first()
        }

        tvPersonalityType.text = title
        tvPersonalityDescription.text = description
    }

    private fun bindTraits() {
        traitsContainer.removeAllViews()
        val sortedTraits = scores.toList().sortedByDescending { it.second }.take(5) // Top 5 traits only
        sortedTraits.forEach { (trait, value) ->
            addTraitRow(trait.replace("_", " ").replaceFirstChar { it.uppercaseChar() }, value)
        }
    }

    private fun addTraitRow(label: String, value: Int) {
        val textView = TextView(this).apply {
            text = "$label: $value%"
            setTextColor(ContextCompat.getColor(this@PersonalityCardActivity, R.color.text_medium))
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = value.coerceIn(0, 100)
            progressTintList = ContextCompat.getColorStateList(this@PersonalityCardActivity, R.color.neon_pink)
            progressBackgroundTintList = ContextCompat.getColorStateList(this@PersonalityCardActivity, R.color.progress_track_dark)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 14).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        traitsContainer.addView(textView)
        traitsContainer.addView(progressBar)
    }

    private fun saveCardToGallery() {
        try {
            val bitmap = Bitmap.createBitmap(cardContainer.width, cardContainer.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            cardContainer.draw(canvas)

            val filename = "personality_card_${System.currentTimeMillis()}.png"
            val uriString = MediaStore.Images.Media.insertImage(contentResolver, bitmap, filename, "Personality Card")
            val uri = uriString?.let { Uri.parse(it) }

            if (uri != null) {
                Toast.makeText(this, "Card saved to gallery!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save card.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCardImage() {
        try {
            val bitmap = Bitmap.createBitmap(cardContainer.width, cardContainer.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            cardContainer.draw(canvas)

            val uriString = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "personality_card_share", null)
            val uri = uriString?.let { Uri.parse(it) }

            if (uri == null) {
                Toast.makeText(this, "Error preparing share image.", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Personality Card"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
