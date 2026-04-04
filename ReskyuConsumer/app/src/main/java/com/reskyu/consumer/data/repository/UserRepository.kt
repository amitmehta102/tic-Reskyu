package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection(Constants.COLLECTION_USERS)

    suspend fun createUserProfile(user: User) {
        usersRef.document(user.uid).set(user).await()
    }

    suspend fun getUserProfile(uid: String): User? {
        return usersRef.document(uid).get().await().toObject(User::class.java)
    }

    fun observeUserProfile(uid: String): Flow<User?> = callbackFlow {
        val reg = usersRef.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateProfile(uid: String, name: String, phone: String) {
        usersRef.document(uid).update(
            mapOf("name" to name, "phone" to phone)
        ).await()
    }

    suspend fun updateImpactStats(uid: String, amountSaved: Double) {
        usersRef.document(uid).update(
            mapOf(
                "impactStats.totalMealsRescued" to
                    com.google.firebase.firestore.FieldValue.increment(1),
                "impactStats.co2SavedKg" to
                    com.google.firebase.firestore.FieldValue.increment(Constants.CO2_PER_MEAL_KG),
                "impactStats.moneySaved" to
                    com.google.firebase.firestore.FieldValue.increment(amountSaved)
            )
        ).await()
    }

    suspend fun saveFcmToken(uid: String, token: String) {
        usersRef.document(uid)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun updateNotificationPrefs(uid: String, prefs: List<String>) {
        usersRef.document(uid).update("notificationPrefs", prefs).await()
    }

    suspend fun updateDiscoveryRadius(uid: String, radiusKm: Int) {
        usersRef.document(uid).update("discoveryRadiusKm", radiusKm).await()
    }

    suspend fun fetchPrivacyPolicy(): String? {
        return try {
            db.collection("config")
                .document("privacy_policy")
                .get()
                .await()
                .getString("content")
        } catch (e: Exception) { null }
    }
}
