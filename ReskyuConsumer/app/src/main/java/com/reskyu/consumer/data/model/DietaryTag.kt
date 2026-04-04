package com.reskyu.consumer.data.model

enum class DietaryTag {
    VEG,
    NON_VEG,
    VEGAN,
    JAIN,       // kept for Firestore backward-compat; hidden from UI chips
    BAKERY,
    SWEETS
}
