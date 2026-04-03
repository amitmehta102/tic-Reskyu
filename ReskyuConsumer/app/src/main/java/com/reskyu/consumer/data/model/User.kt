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
 *  - impactStats   : Aggregate environmental/savings stats (computed on profile view)
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val impactStats: ImpactStats = ImpactStats()
) {
    /** No-arg constructor required by Firestore deserialization */
    constructor() : this("", "", "", "", ImpactStats())
}
