package com.reskyu.consumer.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val consumerType: String = "INDIVIDUAL",
    val fcmToken: String = "",
    val impactStats: ImpactStats = ImpactStats(),
    
    val notificationPrefs: List<String> = emptyList(),
    
    val discoveryRadiusKm: Int = 2
) {
    
    constructor() : this("", "", "", "", "INDIVIDUAL", "", ImpactStats(), emptyList(), 2)
}
