package com.example.nextstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.nextstep.ui.setOnClickWithSound

class ProfileCreationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etFullName: EditText
    private lateinit var etAge: EditText
    private lateinit var tvSelectedAvatarLabel: TextView
    private lateinit var btnCreateProfile: Button
    private lateinit var imgCurrentAvatar: ImageView

    private var selectedAvatarName: String = "avatar_1"

    private val TAG = "ProfileCreationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_creation)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        etFullName = findViewById(R.id.etFullName)
        etAge = findViewById(R.id.etAge)
        tvSelectedAvatarLabel = findViewById(R.id.tvSelectedAvatarLabel)
        btnCreateProfile = findViewById(R.id.btnCreateProfile)
        imgCurrentAvatar = findViewById(R.id.imgCurrentAvatar)

        val avatarviews = listOf(
            findViewById<ImageView>(R.id.avatar1),
            findViewById<ImageView>(R.id.avatar2),
            findViewById<ImageView>(R.id.avatar3),
            findViewById<ImageView>(R.id.avatar4),
            findViewById<ImageView>(R.id.avatar5)
        )
        val avatarNames = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5")

        // Avatar selection logic
        avatarviews.forEachIndexed { idx, view ->
            view.setOnClickListener {
                avatarviews.forEach { it.background = null }
                view.setBackgroundResource(R.drawable.avatar_selector_background)
                selectedAvatarName = avatarNames[idx]
                imgCurrentAvatar.setImageDrawable(view.drawable)
                tvSelectedAvatarLabel.text = "Selected: Avatar ${idx + 1}"
            }
        }

        btnCreateProfile.setOnClickWithSound {
            createAnonymousProfile()
        }
    }

    private fun createAnonymousProfile() {
        val username = etFullName.text.toString().trim()
        val ageText = etAge.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter your full name.", Toast.LENGTH_SHORT).show()
            return
        }
        if (ageText.isEmpty()) {
            Toast.makeText(this, "Please enter your age.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        val userData = hashMapOf(
                            "fullName" to username,
                            "age" to ageText.toIntOrNull(),
                            "avatar" to selectedAvatarName,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "personalityScores" to hashMapOf(
                                "assertiveness" to 0,
                                "empathy" to 0,
                                "leadership" to 0,
                                "adaptability" to 0,
                                "analytical_thinking" to 0,
                                "creativity" to 0,
                                "stress_tolerance" to 0,
                                "teamwork" to 0,
                                "responsibility" to 0,
                                "attention_to_detail" to 0,
                                "problem_solving" to 0
                            )
                        )
                        db.collection("users").document(firebaseUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User profile created for ${firebaseUser.uid}")
                                Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeDashboardActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error creating user profile", e)
                                Toast.makeText(this, "Error creating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
