package com.reskyu.consumer.data.model

/**
 * User
 *
 * Represents a consumer document in the Firestore `/users/{uid}` collection.
 *
 * Firestore path: /users/{uid}
 * Fields:
 *  - uid           : Firebase Auth UID (same as document ID)
 *  - name          : Display name of the user
 *  - email         : Email address
 *  - phone         : Optional phone number
 *  - consumerType  : "INDIVIDUAL" | "NGO" — chosen at sign-up
 *  - fcmToken      : Firebase Cloud Messaging device token — saved on login,
 *                    refreshed by ReskuMessagingService.onNewToken()
 *                    Used by the Node.js backend to send push notifications.
 *  - impactStats   : Aggregate environmental/savings stats (incremented on claim)
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val consumerType: String = "INDIVIDUAL",
    val fcmToken: String = "",
    val impactStats: ImpactStats = ImpactStats(),
    /** DietaryTag names the user wants push notifications for. Empty = all. */
    val notificationPrefs: List<String> = emptyList(),
    /** Discovery radius in km — drives the GeoHash listing query. Default 2km. */
    val discoveryRadiusKm: Int = 2
) {
    /** No-arg constructor required by Firestore deserialization */
    constructor() : this("", "", "", "", "INDIVIDUAL", "", ImpactStats(), emptyList(), 2)
}
