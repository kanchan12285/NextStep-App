package com.example.nextstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlayOnlineActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var roomListener: ListenerRegistration? = null

    // Audio
    private var clickPlayer: android.media.MediaPlayer? = null
    private var themePlayer: android.media.MediaPlayer? = null

    // NEW: guard to avoid double navigation
    private var navigated = false

    private fun currentUid(): String =
        FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_online)

        // Sounds
        clickPlayer = android.media.MediaPlayer.create(this, R.raw.button_click)
        themePlayer = android.media.MediaPlayer.create(this, R.raw.app_theme).apply { isLooping = true }

        findViewById<Button>(R.id.btnCancelSearch).setOnClickListener {
            playClick()
            cancelSearch()
        }
        findViewById<Button?>(R.id.btnFindOpponent)?.setOnClickListener {
            playClick()
            startMatchmaking()
        }

        // Optional: check for an existing/stale room first so we don’t jump unexpectedly
        val uid = currentUid()
        checkExistingRoom(
            uid,
            onFound = { roomId ->
                android.util.Log.d("MM", "Resume existing room=$roomId")
                openRoom(roomId)                    // no return here
            },
            onNotFound = {
                startMatchmaking()                   // start only if no room
            }
        )
    }
    override fun onResume() {
        super.onResume()
        if (themePlayer?.isPlaying != true) themePlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        if (themePlayer?.isPlaying == true) themePlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        roomListener?.remove()
        clickPlayer?.release(); clickPlayer = null
        themePlayer?.release(); themePlayer = null
    }

    private fun playClick() {
        clickPlayer?.let { mp ->
            if (mp.isPlaying) mp.seekTo(0)
            mp.start()
        }
    }

    private fun startMatchmaking() {
        val uid = currentUid()
        findViewById<TextView?>(R.id.tvMatchStatus)?.text = "Looking for an opponent…"
        lifecycleScope.launch {
            try {
                val roomId = claimOrPair(uid) // may be null
                listenForAssignedRoom(uid)
                if (roomId != null) {
                    Log.d("MM", "Paired immediately -> $roomId")
                    openRoom(roomId)
                } else {
                    Log.d("MM", "Enqueued; waiting for pair")
                }
            } catch (e: Exception) {
                Log.e("MM", "Matchmaking error", e)
            }
        }
    }

    // Document-based transaction: /matchmaking/online
    private suspend fun claimOrPair(uid: String): String? {
        val mmRef = db.collection("matchmaking").document("online")
        return db.runTransaction { tx ->
            val snap = tx.get(mmRef)
            val waiting = snap.getString("waitingUid")
            if (waiting == null || waiting == uid) {
                tx.set(
                    mmRef,
                    mapOf("waitingUid" to uid, "lastUpdated" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                null
            } else {
                val roomRef = db.collection("rooms").document()
                tx.set(
                    roomRef,
                    mapOf(
                        "mode" to "online",
                        "status" to "playing",
                        "playerIds" to listOf(waiting, uid),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(mmRef, mapOf("waitingUid" to null))
                roomRef.id
            }
        }.await()
    }

    // NEW: precheck for existing rooms that already include this user
    private fun checkExistingRoom(
        uid: String,
        onFound: (String) -> Unit,
        onNotFound: () -> Unit
    ) {
        db.collection("rooms")
            .whereArrayContains("playerIds", uid)
            .whereIn("status", listOf("playing","pending")) // may require composite index
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                if (!qs.isEmpty) onFound(qs.documents.first().id) else onNotFound()
            }
            .addOnFailureListener { _ -> onNotFound() }     // fall back to matchmaking
    }

    private fun listenForAssignedRoom(uid: String) {
        roomListener?.remove()
        roomListener = db.collection("rooms")
            .whereEqualTo("mode", "online")
            .whereEqualTo("status", "playing")
            .whereArrayContains("playerIds", uid)
            .addSnapshotListener { snap, e ->
                Log.d("MM", "listener err=${e?.code} size=${snap?.size()}")
                val id = snap?.documents?.firstOrNull()?.id ?: return@addSnapshotListener
                openRoom(id)
            }
    }

    private fun openRoom(roomId: String) {
        if (navigated) return
        navigated = true
        roomListener?.remove()
        startActivity(Intent(this, MiniGamesListActivity::class.java).putExtra("ROOM_ID", roomId))
        finish()
    }

    private fun cancelSearch() {
        val uid = currentUid()
        db.collection("matchmaking").document("online")
            .get()
            .addOnSuccessListener { s ->
                if (s.getString("waitingUid") == uid) {
                    db.collection("matchmaking").document("online")
                        .update("waitingUid", null)
                }
            }
        roomListener?.remove()
        finish()
    }
}
