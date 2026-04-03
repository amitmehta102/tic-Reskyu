package com.reskyu.merchant.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.MerchantAuthState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles Firebase Auth sign-in / sign-out and resolves whether the authenticated
 * user has a corresponding document in Firestore /merchants/{uid}.
 */
class MerchantAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val merchantsCollection = firestore.collection("merchants")

    /**
     * Emits the real-time [MerchantAuthState] by combining Firebase Auth state
     * with a Firestore document check for /merchants/{uid}.
     */
    fun observeAuthState(): Flow<MerchantAuthState> = callbackFlow {
        trySend(MerchantAuthState.Loading)

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(MerchantAuthState.Unauthenticated)
            } else {
                merchantsCollection.document(user.uid).get()
                    .addOnSuccessListener { snapshot ->
                        val merchant = snapshot.toObject(Merchant::class.java)
                        if (merchant != null) {
                            trySend(MerchantAuthState.Authenticated(merchant))
                        } else {
                            trySend(MerchantAuthState.NeedsOnboarding)
                        }
                    }
                    .addOnFailureListener {
                        trySend(MerchantAuthState.NeedsOnboarding)
                    }
            }
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in with email and password.
     * @return The Firebase Auth UID on success.
     */
    suspend fun signIn(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("Sign-in succeeded but UID is null")
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Returns the current user's UID or null if unauthenticated.
     */
    fun getCurrentUid(): String? = auth.currentUser?.uid
}
