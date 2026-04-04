package com.reskyu.merchant.data.model

/**
 * Category of a Mystery Box — determines what kind of surplus food is packed inside.
 */
enum class MysteryBoxType(val label: String, val emoji: String) {
    BAKED_GOODS   ("Baked Goods",    "🍞"),
    FULL_MEALS    ("Full Meals",     "🍱"),
    MIXED_SURPRISE("Mixed Surprise", "🎲"),
    VEGAN         ("Vegan",          "🌿"),
    BAKERY        ("Bakery",         "🥐")
}
