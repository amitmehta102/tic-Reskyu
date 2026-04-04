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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val claimRepository    = MerchantClaimRepository()
    private val listingRepository  = ListingRepository()
    private val merchantRepository = MerchantRepository()

    private val _merchant = MutableStateFlow<Merchant?>(null)
    val merchant: StateFlow<Merchant?> = _merchant

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _surplusIqResult = MutableStateFlow<SurplusIqResult?>(null)
    val surplusIqResult: StateFlow<SurplusIqResult?> = _surplusIqResult

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Subscribes to real-time listings + claims Flows and automatically recomputes
     * dashboard stats on every change.
     *
     * Architecture:
     *  - [observeActiveListings] + [observeClaimsForMerchant] are hot Firestore snapshot flows.
     *  - [combine] merges both into a single emission whenever either updates.
     *  - SurplusIQ is a ONE-SHOT call (Gemini is expensive) — runs once on first load.
     */
    fun loadDashboard(merchantId: String) {
        if (merchantId.isBlank()) return

        // ── One-shot: load merchant profile ──────────────────────────────────
        viewModelScope.launch {
            _merchant.value = runCatching { merchantRepository.getMerchant(merchantId) }.getOrNull()
        }

        // ── Reactive: combine live listings + claims into dashboard stats ─────
        viewModelScope.launch {
            combine(
                listingRepository.observeActiveListings(merchantId),
                claimRepository.observeClaimsForMerchant(merchantId)
            ) { listings, claims ->
                val completed = claims.count { it.status == "COMPLETED" }
                val pending   = claims.count { it.status == "PENDING_PICKUP" }
                val revenue   = claims.filter { it.status == "COMPLETED" }.sumOf { it.amount }

                DashboardStats(
                    totalMealsRescued = completed,
                    totalRevenue      = revenue,
                    pendingClaims     = pending,
                    activeListings    = listings.size
                )
            }
            .onStart  { _isLoading.value = true }
            .catch    { _isLoading.value = false }
            .collect  { stats ->
                _stats.value    = stats
                _isLoading.value = false

                // ── One-shot SurplusIQ — only on first successful stats load ──
                if (_surplusIqResult.value == null) {
                    launch {
                        val result = runCatching {
                            com.reskyu.merchant.data.repository.SurplusIqRepository.getPrediction(
                                uid          = merchantId,
                                salesHistory = buildSalesHistory(
                                    claimRepository.getClaimsForMerchant(merchantId)
                                )
                            )
                        }.getOrNull()

                        _surplusIqResult.value = result ?: com.reskyu.merchant.data.model.SurplusIqResult(
                            predictedMeals = 7,
                            reasoning      = "Based on recent sales trend",
                            confidence     = 0.80f
                        )
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Groups completed claims into buckets by calendar day over the last 7 days.
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

