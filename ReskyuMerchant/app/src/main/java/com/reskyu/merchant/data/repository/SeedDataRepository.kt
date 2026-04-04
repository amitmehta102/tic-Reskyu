package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Seeds realistic fake claim data into Firestore for demo / testing purposes.
 *
 * Creates claims in /claims collection spread across the last 7 days,
 * with varied meal counts & amounts to make analytics charts look populated.
 *
 * Call once from the ESG screen when the merchant has no data yet.
 */
object SeedDataRepository {

    private val firestore  = FirebaseFirestore.getInstance()
    private val claimsCol  = firestore.collection("claims")
    private val listingsCol = firestore.collection("listings")

    // Realistic business names for dummy consumers
    private val consumerNames = listOf(
        "Priya Sharma", "Rahul Mehta", "Ananya Gupta", "Vikram Singh",
        "Sneha Patel", "Arjun Nair", "Kavya Reddy", "Mohit Joshi",
        "Ritu Verma", "Aditya Kumar", "Deepika Rao", "Sanjay Iyer"
    )

    private val foodItems = listOf(
        "Veg Thali", "Paneer Wrap", "Biryani Box", "Masala Dosa", "Chole Bhature",
        "Dal Makhani", "Rajma Rice", "Pav Bhaji", "Idli Sambhar", "Aloo Paratha"
    )

    /**
     * Seeds 7 days of fake claim data for [merchantId].
     * Each day gets 3-9 completed claims with realistic prices.
     * Also seeds one DISPUTED claim per 2 days for realistic recovery-rate charts.
     *
     * Safe to call multiple times — existing data is NOT deleted.
     */
    suspend fun seedDemoData(merchantId: String, businessName: String) {
        val batch = firestore.batch()
        val zone  = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Day distribution — crescendo then slight dip (realistic trend)
        val mealsPerDay = listOf(4, 6, 5, 8, 7, 9, 6)  // 7 days ago → today
        val prices      = listOf(79.0, 99.0, 119.0, 149.0, 89.0, 129.0, 109.0)

        for (dayOffset in 0..6) {
            val date   = today.minusDays((6 - dayOffset).toLong())
            val dayMs  = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val mealCount = mealsPerDay[dayOffset]

            // Seed a fake listing for this day
            val listingId = "demo_listing_${merchantId}_day$dayOffset"
            val listingData = hashMapOf(
                "merchantId"      to merchantId,
                "businessName"    to businessName,
                "heroItem"        to foodItems.random(),
                "originalPrice"   to prices[dayOffset] * 1.5,
                "discountedPrice" to prices[dayOffset],
                "mealsAvailable"  to mealCount + 2,
                "mealsClaimed"    to mealCount,
                "status"          to "EXPIRED",
                "imageUrl"        to "",
                "timestamp"       to dayMs + 36_000_000L,  // 10am
                "expiresAt"       to dayMs + 57_600_000L   // 4pm
            )
            batch.set(listingsCol.document(listingId), listingData)

            // Seed completed claims for this day
            repeat(mealCount) { claimIdx ->
                val claimMs  = dayMs + Random.nextLong(36_000_000L, 57_600_000L)
                val claimRef = claimsCol.document()
                val claimData = hashMapOf(
                    "merchantId"   to merchantId,
                    "businessName" to businessName,
                    "listingId"    to listingId,
                    "consumerId"   to "demo_user_${claimIdx % consumerNames.size}",
                    "consumerName" to consumerNames[claimIdx % consumerNames.size],
                    "heroItem"     to foodItems[dayOffset % foodItems.size],
                    "amount"       to prices[dayOffset],
                    "status"       to "COMPLETED",
                    "timestamp"    to claimMs
                )
                batch.set(claimRef, claimData)
            }

            // Seed 1 DISPUTED claim every 2 days for realistic recovery rate
            if (dayOffset % 2 == 0) {
                val claimMs  = dayMs + Random.nextLong(36_000_000L, 57_600_000L)
                val claimRef = claimsCol.document()
                val disputed = hashMapOf(
                    "merchantId"   to merchantId,
                    "businessName" to businessName,
                    "listingId"    to listingId,
                    "consumerId"   to "demo_user_disputed_$dayOffset",
                    "consumerName" to consumerNames[(dayOffset + 3) % consumerNames.size],
                    "heroItem"     to foodItems[dayOffset % foodItems.size],
                    "amount"       to prices[dayOffset],
                    "status"       to "DISPUTED",
                    "timestamp"    to claimMs
                )
                batch.set(claimRef, disputed)
            }
        }

        batch.commit().await()
    }

    /**
     * Checks if the merchant already has claim data in Firestore.
     * Returns true if they have at least 1 claim document.
     */
    suspend fun hasExistingData(merchantId: String): Boolean {
        val snap = claimsCol
            .whereEqualTo("merchantId", merchantId)
            .limit(1)
            .get()
            .await()
        return !snap.isEmpty
    }
}
