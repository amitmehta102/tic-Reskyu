package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.MerchantDraft
import com.reskyu.merchant.data.model.ListingTemplate
import kotlinx.coroutines.tasks.await

/**
 * Handles merchant profile creation (onboarding), profile updates,
 * and management of listing templates in Firestore.
 *
 * Firestore paths:
 *  - /merchants/{uid}
 *  - /merchants/{uid}/templates/{templateId}
 */
class MerchantRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val merchantsCollection = firestore.collection("merchants")

    /**
     * Creates the /merchants/{uid} document from an [MerchantDraft].
     * Called at the end of the onboarding flow.
     */
    suspend fun completeMerchantOnboarding(draft: MerchantDraft) {
        val merchant = Merchant(
            uid = draft.uid,
            businessName = draft.businessName,
            closingTime = draft.closingTime,
            lat = draft.lat,
            lng = draft.lng,
            geoHash = draft.geoHash
        )
        merchantsCollection.document(draft.uid).set(merchant).await()
    }

    /**
     * Fetches the merchant profile document from Firestore.
     */
    suspend fun getMerchant(uid: String): Merchant? {
        val snapshot = merchantsCollection.document(uid).get().await()
        return snapshot.toObject(Merchant::class.java)
    }

    /**
     * Updates specific fields on the /merchants/{uid} document.
     */
    suspend fun updateProfile(uid: String, updates: Map<String, Any>) {
        merchantsCollection.document(uid).update(updates).await()
    }

    // ─── Templates Sub-collection ──────────────────────────────────────────────

    /**
     * Saves or overwrites a listing template in /merchants/{uid}/templates/{templateId}.
     */
    suspend fun saveTemplate(uid: String, template: ListingTemplate) {
        val docRef = if (template.templateId.isEmpty()) {
            merchantsCollection.document(uid).collection("templates").document()
        } else {
            merchantsCollection.document(uid).collection("templates").document(template.templateId)
        }
        docRef.set(template.copy(templateId = docRef.id)).await()
    }

    /**
     * Fetches all listing templates for the given merchant.
     */
    suspend fun getTemplates(uid: String): List<ListingTemplate> {
        val snapshot = merchantsCollection.document(uid).collection("templates").get().await()
        return snapshot.toObjects(ListingTemplate::class.java)
    }

    /**
     * Deletes a listing template by ID.
     */
    suspend fun deleteTemplate(uid: String, templateId: String) {
        merchantsCollection.document(uid).collection("templates").document(templateId).delete().await()
    }
}
