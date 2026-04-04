package com.reskyu.consumer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.remote.GeminiApiClient
import com.reskyu.consumer.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ListingDetailViewModel
 *
 * Loads a single listing and generates an AI insight.
 *
 * Insight strategy — in priority order:
 *
 *  1. FIRESTORE CACHE — if `/listings/{id}/aiInsight` already exists, use it.
 *     This means Gemini is only called ONCE per listing, ever.
 *
 *  2. GEMINI API — if no cache, call gemini-2.0-flash (1,500 RPD free).
 *     On success, save result back to Firestore so future views are free.
 *
 *  3. RULE-BASED FALLBACK — if Gemini is rate-limited or offline, generate
 *     a deterministic insight from the listing's own data. Zero API calls,
 *     always works, still looks good in the UI.
 */
class ListingDetailViewModel : ViewModel() {

    private val listingRepository = ListingRepository()
    private val db                = FirebaseFirestore.getInstance()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _geminiInsight = MutableStateFlow<String?>(null)
    val geminiInsight: StateFlow<String?> = _geminiInsight.asStateFlow()

    private val _insightLoading = MutableStateFlow(false)
    val insightLoading: StateFlow<Boolean> = _insightLoading.asStateFlow()

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = listingRepository.getListingById(listingId)
                _listing.value = result
                result?.let { generateInsightCached(it) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load listing"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cache-first insight generation:
     *  • Read cached value from Firestore → instant, no API call
     *  • If missing, call Gemini → save result to Firestore for future
     *  • If Gemini fails → use local rule-based insight
     */
    private fun generateInsightCached(listing: Listing) {
        viewModelScope.launch {
            _insightLoading.value = true

            try {
                // ── Step 1: Check Firestore cache ───────────────────────────
                val doc = db.collection("listings").document(listing.id).get().await()
                val cached = doc.getString("aiInsight")

                if (!cached.isNullOrBlank()) {
                    // Cache hit — zero Gemini calls
                    _geminiInsight.value = cached
                    _insightLoading.value = false
                    return@launch
                }

                // ── Step 2: Call Gemini (cache miss) ────────────────────────
                val aiResult = GeminiApiClient.generateListingInsight(
                    heroItem     = listing.heroItem,
                    dietaryTag   = listing.dietaryTag,
                    businessName = listing.businessName
                )

                val insight = if (!aiResult.isNullOrBlank()) {
                    // Save to Firestore so next view of this listing is free
                    db.collection("listings").document(listing.id)
                        .update("aiInsight", aiResult)
                    aiResult
                } else {
                    // ── Step 3: Rule-based fallback ─────────────────────────
                    ruleBasedInsight(listing)
                }

                _geminiInsight.value = insight

            } catch (_: Exception) {
                // Any Firestore/Network error → still show fallback
                _geminiInsight.value = ruleBasedInsight(listing)
            } finally {
                _insightLoading.value = false
            }
        }
    }

    /**
     * Generates a deterministic insight from listing data — no API needed.
     * Uses discount %, dietary tag, and meals remaining for context.
     * Rotates through varied templates so it doesn't feel repetitive.
     */
    private fun ruleBasedInsight(listing: Listing): String {
        val discount = if (listing.originalPrice > 0)
            ((1 - listing.discountedPrice / listing.originalPrice) * 100).toInt() else 0
        val dietEmoji = when (listing.dietaryTag) {
            "VEG"     -> "🥗"
            "VEGAN"   -> "🌱"
            "NON_VEG" -> "🍗"
            "JAIN"    -> "🙏"
            else      -> "🍽️"
        }
        val urgency = when {
            listing.mealsLeft <= 1 -> "Only 1 left"
            listing.mealsLeft <= 3 -> "Only ${listing.mealsLeft} left"
            else                   -> "${listing.mealsLeft} portions available"
        }
        val templates = listOf(
            "Save $discount% and rescue this meal — $urgency — every bite fights food waste! $dietEmoji",
            "Skip the bin, grab the deal! $discount% off ${listing.heroItem} — $urgency. $dietEmoji",
            "Rescuing this saves CO₂ and money — $discount% off, $urgency. Don't miss it! $dietEmoji",
            "Good food deserves a good home! Grab ${listing.heroItem} at $discount% off. $urgency $dietEmoji",
            "Fight food waste one meal at a time — $discount% off, $urgency! $dietEmoji"
        )
        // Pick template based on listing ID hash so it's always the same for a given listing
        return templates[abs(listing.id.hashCode()) % templates.size]
    }

    private fun abs(n: Int) = if (n < 0) -n else n
}
