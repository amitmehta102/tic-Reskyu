package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * UserRepository
 *
 * Handles all Firestore reads/writes to the `/users` collection.
 *
 * Key Methods:
 *  [createUserProfile]     Write a new user document on first sign-up (includes consumerType)
 *  [getUserProfile]        One-shot profile read
 *  [observeUserProfile]    Real-time Flow for profile screen
 *  [updateProfile]         Write name and phone back to Firestore
 *  [updateImpactStats]     Atomic increment after a completed claim
 */
class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection(Constants.COLLECTION_USERS)

    /**
     * Creates a new user document in `/users/{uid}`.
     * Called once after successful registration.
     * Includes the consumerType chosen on the sign-up screen.
     */
    suspend fun createUserProfile(user: User) {
        usersRef.document(user.uid).set(user).await()
    }

    /**
     * One-shot fetch of the user's Firestore profile.
     */
    suspend fun getUserProfile(uid: String): User? {
        return usersRef.document(uid).get().await().toObject(User::class.java)
    }

    /**
     * Real-time Flow of the user's profile document.
     * Emits a new [User] every time the Firestore document changes.
     * Used by ProfileViewModel to keep the screen up-to-date.
     */
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

    /**
     * Updates the user's editable fields (name, phone).
     */
    suspend fun updateProfile(uid: String, name: String, phone: String) {
        usersRef.document(uid).update(
            mapOf("name" to name, "phone" to phone)
        ).await()
    }

    /**
     * Atomically increments ImpactStats after a completed claim.
     * CO₂ formula: ~2.5 kg CO₂ saved per meal rescued (WRAP report estimate).
     *
     * @param uid         Firebase Auth UID
     * @param amountSaved INR saved on this claim (originalPrice - discountedPrice)
     */
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

    /**
     * Saves (or refreshes) the device's FCM token to the user's Firestore document.
     *
     * Called from two places to guarantee the token is always current:
     *  1. [LoginViewModel] — after every successful sign-in and sign-up
     *  2. [ReskuMessagingService.onNewToken()] — when FCM rotates the token
     *
     * Uses update() so only the fcmToken field is written; other fields are untouched.
     */
    suspend fun saveFcmToken(uid: String, token: String) {
        // CRITICAL: use set+merge, NOT update().
        // update() throws DocumentNotFoundException if fcmToken field doesn't exist yet
        // (first login / fresh install / data cleared). That exception is swallowed
        // silently in LoginViewModel, meaning the token is never written → backend
        // query returns no tokens → notifications never delivered.
        usersRef.document(uid)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Saves the user's selected dietary notification preferences.
     * An empty list means "notify me for everything".
     */
    suspend fun updateNotificationPrefs(uid: String, prefs: List<String>) {
        usersRef.document(uid).update("notificationPrefs", prefs).await()
    }

    /**
     * Updates the user's preferred discovery radius (in km).
     * This is passed to the GeoHash listing query on next refresh.
     */
    suspend fun updateDiscoveryRadius(uid: String, radiusKm: Int) {
        usersRef.document(uid).update("discoveryRadiusKm", radiusKm).await()
    }

    /**
     * One-shot fetch of the app's privacy policy from Firestore.
     * Document path: config/privacy_policy
     * Field: content (plain text)
     *
     * The document must be added manually via the Firebase Console.
     */
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
