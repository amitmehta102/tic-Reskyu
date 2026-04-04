package com.reskyu.consumer.data.model

import com.google.firebase.Timestamp

data class Listing(
    val id: String = "",
    val merchantId: String = "",
    val businessName: String = "",
    val heroItem: String = "",
    val dietaryTag: String = DietaryTag.VEG.name,
    val mealsLeft: Int = 0,
    val originalPrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val imageUrl: String = "",
    val geoHash: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val expiresAt: Timestamp = Timestamp.now(),
    val status: String = ListingStatus.OPEN.name,

    val listingType: String = "STANDARD",
    
    val boxType: String = "",
    
    val priceRangeMin: Double = 0.0,
    
    val priceRangeMax: Double = 0.0,
    
    val itemCount: Int = 0
) {
    
    val isMysteryBox: Boolean get() = listingType == "MYSTERY_BOX"

    constructor() : this(
        "", "", "", "", DietaryTag.VEG.name, 0, 0.0, 0.0, "", "", 0.0, 0.0,
        Timestamp.now(), ListingStatus.OPEN.name,
        "STANDARD", "", 0.0, 0.0, 0
    )
}
