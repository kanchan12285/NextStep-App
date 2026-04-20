package com.example.nextstep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        if (user == null) {
            // Sign in anonymously if no user found
            auth.signInAnonymously()
                .addOnSuccessListener {
                    checkProfile(auth.currentUser?.uid)
                }
                .addOnFailureListener {
                    // Handle failure gracefully (finish activity)
                    finish()
                }
        } else {
            checkProfile(user.uid)
        }
    }

    private fun checkProfile(uid: String?) {
        if (uid == null) {
            // Cannot proceed without valid UID
            finish()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User profile exists, open home dashboard
                    startActivity(Intent(this, HomeDashboardActivity::class.java))
                } else {
                    // No profile found, open profile creation
                    startActivity(Intent(this, ProfileCreationActivity::class.java))
                }
                finish() // Close current activity after navigation
            }
            .addOnFailureListener {
                // On error, default to profile creation activity
                startActivity(Intent(this, ProfileCreationActivity::class.java))
                finish()
            }
    }
}
