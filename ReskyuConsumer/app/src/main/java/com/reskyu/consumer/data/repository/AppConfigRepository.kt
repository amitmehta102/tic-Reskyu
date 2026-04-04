package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AppConfigRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Emits the minimum required versionCode from Firestore in real-time.
     * Returns 1 (no force update) on any error so the app never gets
     * accidentally blocked if Firestore is unreachable.
     *
     * Firestore path: config/app_config  →  field: minVersionCode (Number)
     */
    fun observeMinVersionCode(): Flow<Int> = callbackFlow {
        val reg = db.collection("config")
            .document("app_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(1)   // fail open
                    return@addSnapshotListener
                }
                val min = (snapshot.getLong("minVersionCode") ?: 1L).toInt()
                trySend(min)
            }
        awaitClose { reg.remove() }
    }
}
