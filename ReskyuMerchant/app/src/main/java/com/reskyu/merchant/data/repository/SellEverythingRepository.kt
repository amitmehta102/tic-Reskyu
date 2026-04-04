package com.reskyu.merchant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.reskyu.merchant.data.model.DietaryTag
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.ListingStatus
import com.reskyu.merchant.data.model.ListingType
import com.reskyu.merchant.data.model.MysteryBoxType
import com.reskyu.merchant.data.model.SellEverythingContext
import com.reskyu.merchant.data.model.SellEverythingResult
import com.reskyu.merchant.data.remote.GeminiApiService
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Orchestrates the "Sell Everything Mode" feature.
 *
 * Responsibilities:
 *  1. Compute minutes-until-close from merchant's closing time
 *  2. Aggregate active listing data (mealsLeft, avg price, hero item)
 *  3. Compute historical sell-out rate from claims
 *  4. Call [GeminiApiService.planSellEverything] for AI strategy
 *  5. Apply discounts to all active listings ([applyDiscountToListings])
 *  6. Auto-create a MYSTERY_BOX bundle listing ([createBundleListing])
 *  7. Persist sell-out mode metadata to /merchants/{uid} doc
 */
class SellEverythingRepository {

    private val db             = FirebaseFirestore.getInstance()
    private val listingsCol    = db.collection("listings")
    private val claimsCol      = db.collection("claims")
    private val merchantsCol   = db.collection("merchants")

    private val listingRepo    = ListingRepository()

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes how many minutes remain until the merchant's closing time.
     *
     * @param closingTime  "HH:mm" string from Firestore (e.g. "22:00")
     * @return Minutes until close, or -1 if format is missing/invalid.
     */
    fun minutesUntilClose(closingTime: String): Int {
        if (closingTime.isBlank()) return -1
        return try {
            val fmt     = DateTimeFormatter.ofPattern("HH:mm")
            val close   = LocalTime.parse(closingTime, fmt)
            val now     = LocalTime.now()
            val diffMin = java.time.Duration.between(now, close).toMinutes().toInt()
            // If closing is before current time, mode is past — return 0
            diffMin.coerceAtLeast(0)
        } catch (_: DateTimeParseException) { -1 }
    }

    /**
     * Fetches active listings for [merchantId], then builds and returns a
     * [SellEverythingContext] ready to be sent to Gemini.
     *
     * @return null if there are no active listings.
     */
    suspend fun buildContext(
        merchantId:       String,
        minutesUntilClose: Int,
        merchantName:     String
    ): SellEverythingContext? {
        val listings = getActiveListings(merchantId)
        if (listings.isEmpty()) return null

        val totalMeals   = listings.sumOf { it.mealsLeft }
        val avgPrice     = listings.map { it.originalPrice }.average().let {
            if (it.isNaN()) 0.0 else it
        }
        val topHeroItem  = listings
            .groupingBy { it.heroItem.trim().lowercase() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.let { listings.first { l -> l.heroItem.trim().lowercase() == it.key }.heroItem }
            ?: ""

        val sellOutRate  = computeHistoricalSellOutRate(merchantId)

        return SellEverythingContext(
            mealsLeft            = totalMeals,
            minutesUntilClose    = minutesUntilClose,
            originalPriceAvg     = avgPrice,
            historicalSellOutRate = sellOutRate,
            topHeroItem          = topHeroItem,
            merchantName         = merchantName
        )
    }

    /**
     * Applies an AI-computed discount to ALL active listings for [merchantId].
     *
     * Uses [editListing] with the new discountedPrice (= originalPrice * (1 - pct/100)).
     * If the computed price is below ₹1, it floors at ₹1.
     */
    suspend fun applyDiscountToListings(merchantId: String, discountPercentage: Int) {
        val listings = getActiveListings(merchantId)
        val factor   = 1.0 - (discountPercentage / 100.0)
        listings.forEach { listing ->
            val newPrice = (listing.originalPrice * factor).coerceAtLeast(1.0)
            runCatching {
                listingRepo.editListing(
                    listingId = listing.id,
                    updates   = mapOf("discountedPrice" to newPrice)
                )
            }
        }
    }

    /**
     * Creates an auto-generated MYSTERY_BOX bundle listing in Firestore.
     *
     * The bundle is based on the first active listing found (copies its hero item,
     * dietary tag, geoHash, lat, lng and imageUrl). Expires exactly at closing time.
     *
     * @return The Firestore document ID of the created bundle listing.
     */
    suspend fun createBundleListing(
        merchantId:        String,
        businessName:      String,
        geoHash:           String,
        lat:               Double,
        lng:               Double,
        result:            SellEverythingResult,
        minutesUntilClose: Int
    ): String {
        val listings    = getActiveListings(merchantId)
        val sourceListing = listings.firstOrNull()

        // Derive bundle count: floor(totalMeals / bundleMeals), at least 1
        val totalMeals   = listings.sumOf { it.mealsLeft }
        val bundleCount  = (totalMeals / result.bundleMeals).coerceAtLeast(1)

        // Reuse source listing's dietary tag and image where possible
        val dietaryTag = try {
            DietaryTag.valueOf(sourceListing?.dietaryTag ?: DietaryTag.VEG.name)
        } catch (_: IllegalArgumentException) { DietaryTag.VEG }

        val imageUrl     = sourceListing?.imageUrl ?: ""

        // Bundle original price = bundleMeals * avgOriginalPrice; discounted = bundlePrice
        val avgOriginalPrice = listings.map { it.originalPrice }
            .average().let { if (it.isNaN()) result.bundlePrice * 2 else it }
        val bundleOriginalPrice = (avgOriginalPrice * result.bundleMeals)
            .coerceAtLeast(result.bundlePrice + 1.0)

        val form = ListingForm(
            listingType    = ListingType.MYSTERY_BOX,
            heroItem       = "🔥 Sell-Out Bundle (${result.bundleMeals} meals)",
            dietaryTag     = dietaryTag,
            mealsAvailable = bundleCount,
            originalPrice  = bundleOriginalPrice,
            discountedPrice = result.bundlePrice,
            imageUrl       = imageUrl,
            expiresInMinutes = minutesUntilClose.coerceAtLeast(5),
            boxType        = MysteryBoxType.FULL_MEALS,
            itemCount      = result.bundleMeals
        )

        return listingRepo.postListing(
            form         = form,
            merchantId   = merchantId,
            businessName = businessName,
            geoHash      = geoHash,
            lat          = lat,
            lng          = lng
        )
    }

    /**
     * Persists sell-out mode metadata to /merchants/{uid} so a Cloud Function
     * can later pick up [pushRequired] and [urgencyMessage] to dispatch push
     * notifications to consumers.
     */
    suspend fun persistSellOutModeState(
        merchantId:     String,
        result:         SellEverythingResult,
        bundleListingId: String
    ) {
        runCatching {
            merchantsCol.document(merchantId).set(
                mapOf(
                    "sellOutModeActive"     to true,
                    "sellOutDiscount"       to result.discountPercentage,
                    "sellOutBundleId"       to bundleListingId,
                    "sellOutUrgencyMessage" to result.urgencyMessage,
                    "sellOutPushRequired"   to result.pushRequired,
                    "sellOutTimestamp"      to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    /**
     * Clears the sell-out mode flag in Firestore (called when mode expires or
     * is manually deactivated).
     */
    suspend fun clearSellOutModeState(merchantId: String) {
        runCatching {
            merchantsCol.document(merchantId).update(
                mapOf("sellOutModeActive" to false)
            ).await()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun getActiveListings(merchantId: String): List<Listing> {
        val activeStatuses = setOf(ListingStatus.OPEN.name, ListingStatus.CLOSING.name)
        return runCatching {
            listingsCol
                .whereEqualTo("merchantId", merchantId)
                .get().await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val status    = doc.getString("status") ?: return@mapNotNull null
                        val expiresAt = doc.getLong("expiresAt") ?: 0L
                        val now       = System.currentTimeMillis()
                        if (status !in activeStatuses || (expiresAt > 0L && expiresAt <= now)) {
                            return@mapNotNull null
                        }
                        Listing(
                            id              = doc.id,
                            merchantId      = doc.getString("merchantId")      ?: "",
                            businessName    = doc.getString("businessName")    ?: "",
                            heroItem        = doc.getString("heroItem")        ?: "",
                            dietaryTag      = doc.getString("dietaryTag")      ?: DietaryTag.VEG.name,
                            mealsLeft       = doc.getLong("mealsLeft")?.toInt() ?: 0,
                            originalPrice   = doc.getDouble("originalPrice")   ?: 0.0,
                            discountedPrice = doc.getDouble("discountedPrice") ?: 0.0,
                            imageUrl        = doc.getString("imageUrl")        ?: "",
                            geoHash         = doc.getString("geoHash")         ?: "",
                            lat             = doc.getDouble("lat")             ?: 0.0,
                            lng             = doc.getDouble("lng")             ?: 0.0,
                            expiresAt       = expiresAt,
                            status          = status
                        )
                    } catch (_: Exception) { null }
                }
        }.getOrElse { emptyList() }
    }

    /**
     * Computes the % of past COMPLETED listings relative to total listings.
     * Returns 0.5 (50%) if there is no history — a neutral default.
     */
    private suspend fun computeHistoricalSellOutRate(merchantId: String): Float {
        return runCatching {
            val claims = claimsCol
                .whereEqualTo("merchantId", merchantId)
                .get().await()
                .documents
            val total     = claims.size
            val completed = claims.count { it.getString("status") == "COMPLETED" }
            if (total == 0) 0.5f else (completed.toFloat() / total).coerceIn(0f, 1f)
        }.getOrElse { 0.5f }
    }
}
