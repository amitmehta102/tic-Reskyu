package com.reskyu.merchant.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.DashboardStats
import com.reskyu.merchant.data.model.Merchant
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.repository.ListingRepository
import com.reskyu.merchant.data.repository.MerchantClaimRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import com.reskyu.merchant.data.repository.SurplusIqRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val claimRepository    = MerchantClaimRepository()
    private val listingRepository  = ListingRepository()
    private val merchantRepository = MerchantRepository()
    private val surplusIqRepo      = SurplusIqRepository()

    private val _merchant = MutableStateFlow<Merchant?>(null)
    val merchant: StateFlow<Merchant?> = _merchant

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _surplusIqResult = MutableStateFlow<SurplusIqResult?>(null)
    val surplusIqResult: StateFlow<SurplusIqResult?> = _surplusIqResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Loads the full dashboard in parallel:
     *  1. Merchant profile  → business name header + SurplusIQ cache keys
     *  2. Claims            → pending / completed counts + revenue
     *  3. Active listings   → activeListings count
     *  4. SurplusIQ         → Gemini prediction (uses Firestore cache, refreshes if stale)
     */
    fun loadDashboard(merchantId: String) {
        if (merchantId.isBlank()) return
        _isLoading.value = true

        viewModelScope.launch {
            // ── Parallel fetch of merchant profile + claims + listings ──────────
            val merchantDeferred  = async { runCatching { merchantRepository.getMerchant(merchantId) }.getOrNull() }
            val claimsDeferred    = async { runCatching { claimRepository.getClaimsForMerchant(merchantId) }.getOrDefault(emptyList()) }
            val listingsDeferred  = async { runCatching { listingRepository.getActiveListings(merchantId) }.getOrDefault(emptyList()) }

            val merchant      = merchantDeferred.await()
            val claims        = claimsDeferred.await()
            val activeCount   = listingsDeferred.await().size

            _merchant.value = merchant

            val completed = claims.count { it.status == "COMPLETED" }
            val pending   = claims.count { it.status == "PENDING_PICKUP" }
            val revenue   = claims.filter { it.status == "COMPLETED" }.sumOf { it.amount }

            _stats.value = DashboardStats(
                totalMealsRescued = completed,
                totalRevenue      = revenue,
                pendingClaims     = pending,
                activeListings    = activeCount
            )

            // ── SurplusIQ (separate try — Gemini can be slow / fail) ───────────
            try {
                val prediction = surplusIqRepo.getPrediction(
                    uid           = merchantId,
                    lastPredDate  = merchant?.lastPredictionDate  ?: "",
                    lastPredMeals = merchant?.lastPredictionMeals ?: 0,
                    salesHistory  = buildSalesHistory(claims)
                )
                _surplusIqResult.value = prediction
            } catch (e: Throwable) {
                // SurplusIQ is optional — banner stays hidden on failure
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Groups completed claims into bucket by calendar day over the last 7 days.
     * Day 0 = 7 days ago, Day 6 = today.
     */
    private fun buildSalesHistory(claims: List<MerchantClaim>): List<Int> {
        val dayMs   = 24L * 60 * 60 * 1000
        val weekAgo = System.currentTimeMillis() - (7 * dayMs)
        return (0..6).map { d ->
            val start = weekAgo + d * dayMs
            claims.count { it.status == "COMPLETED" && it.timestamp in start until start + dayMs }
        }
    }
}
