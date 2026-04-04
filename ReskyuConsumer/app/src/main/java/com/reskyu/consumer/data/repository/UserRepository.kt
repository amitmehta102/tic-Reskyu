package com.reskyu.consumer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.consumer.data.model.User
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.tasks.await

/**
 * UserRepository
 *
 * Handles all Firestore reads/writes to the `/users` collection.
 * Consumer profile data and ImpactStats are managed here.
 *
 * Key Methods:
 *  - createUserProfile()    : Write a new user document on first sign-in
 *  - getUserProfile()       : Read the user's profile document
 *  - updateImpactStats()    : Recompute and update stats after a completed claim
 */
class UserRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef = db.collection(Constants.COLLECTION_USERS)

    /**
     * Creates a new user document in `/users/{uid}`.
     * Call this once after successful registration/first sign-in.
     *
     * @param user  The [User] object to persist
     */
    suspend fun createUserProfile(user: User) {
        usersRef.document(user.uid).set(user).await()
    }

    /**
     * Fetches the user's profile document from Firestore.
     *
     * @param uid  Firebase Auth UID
     * @return     [User] object or null if the document doesn't exist
     */
    suspend fun getUserProfile(uid: String): User? {
        return usersRef
            .document(uid)
            .get()
            .await()
            .toObject(User::class.java)
    }

    /**
     * Updates the user's ImpactStats after a claim is completed.
     * Aggregates totalMealsRescued, co2SavedKg, and moneySaved.
     *
     * CO₂ formula: ~2.5 kg CO₂ saved per meal rescued (configurable).
     *
     * @param uid            Firebase Auth UID
     * @param amountSaved    INR saved on this claim (originalPrice - discountedPrice)
     */
    suspend fun updateImpactStats(uid: String, amountSaved: Double) {
        val co2PerMeal = 2.5 // kg of CO₂ per meal rescued (adjust as needed)
        usersRef.document(uid).update(
            mapOf(
                "impactStats.totalMealsRescued" to com.google.firebase.firestore.FieldValue.increment(1),
                "impactStats.co2SavedKg" to com.google.firebase.firestore.FieldValue.increment(co2PerMeal),
                "impactStats.moneySaved" to com.google.firebase.firestore.FieldValue.increment(amountSaved)
            )
        ).await()
    }

    /**
     * Updates the user's editable profile fields (name, phone).
     */
    suspend fun updateProfile(uid: String, name: String, phone: String) {
        usersRef.document(uid).update(
            mapOf("name" to name, "phone" to phone)
        ).await()
    }
}
