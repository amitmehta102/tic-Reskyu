package com.reskyu.consumer.data.model

data class ImpactStats(
    val totalMealsRescued: Int = 0,
    val co2SavedKg: Double = 0.0,
    val moneySaved: Double = 0.0
) {
    
    constructor() : this(0, 0.0, 0.0)
}
