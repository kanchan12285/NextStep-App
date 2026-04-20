package com.example.nextstep.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class MatchmakingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Returns roomId if paired immediately; null means you are now waiting
    suspend fun enqueueOrPair(uid: String): String? {
        val mmRef = db.collection("matchmaking").document("online")
        return db.runTransaction { tx ->
            val snap = tx.get(mmRef) // DocumentReference read is allowed
            val waiting = snap.getString("waitingUid")

            if (waiting == null || waiting == uid) {
                // No one waiting (or already claimed by me) -> claim slot
                tx.set(
                    mmRef,
                    mapOf(
                        "waitingUid" to uid,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                null
            } else {
                // Someone is waiting -> create a room and clear the slot atomically
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
}
