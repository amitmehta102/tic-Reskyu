package com.reskyu.merchant.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.EsgStats
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.repository.EsgRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import com.reskyu.merchant.data.repository.SurplusIqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class EsgAnalyticsViewModel : ViewModel() {

    private val esgRepository      = EsgRepository()
    private val merchantRepository = MerchantRepository()

    // ── ESG stats — demo data shown instantly, replaced with real on load ──────
    private val _esgStats = MutableStateFlow(demoEsgStats())
    val esgStats: StateFlow<EsgStats> = _esgStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding

    // ── SurplusIQ
    // Default to NewRestaurant(0) — safest initial state for a new merchant.
    // Once real stats load, updateSurplusIqState() decides what to show.
    private val _surplusIq = MutableStateFlow<SurplusIqUiState>(
        SurplusIqUiState.NewRestaurant(mealsRescued = 0)
    )
    val surplusIq: StateFlow<SurplusIqUiState> = _surplusIq

    // Tracks the REAL meal count from Firestore (not demo data).
    // Used to decide whether to show Gemini or the "keep listing" prompt.
    private var realMealsRescued: Int = 0
    private var lastMerchantId = ""

    // ─────────────────────────────────────────────────────────────────────────
    // Public
    // ─────────────────────────────────────────────────────────────────────────

    fun loadStats(merchantId: String) {
        if (merchantId.isBlank()) return
        lastMerchantId = merchantId
        _isLoading.value = true

        viewModelScope.launch {
            // ── Load real ESG stats from Firestore ────────────────────────────
            val realStats = runCatching { esgRepository.getEsgStats(merchantId) }.getOrNull()

            if (realStats != null && realStats.totalMealsRescued > 0) {
                // Merchant has real data — replace demo data with actuals
                _esgStats.value  = realStats
                realMealsRescued = realStats.totalMealsRescued
            } else {
                // No real data yet — keep demo charts for UI appeal,
                // but track real count as 0 for the Gemini threshold check
                realMealsRescued = 0
            }
            _isLoading.value = false

            // ── SurplusIQ decision ────────────────────────────────────────────
            updateSurplusIqState(
                merchantId   = merchantId,
                salesHistory = _esgStats.value.weeklyData.map { it.toInt() },
                isRealData   = realMealsRescued > 0
            )
        }
    }

    /**
     * Forces a fresh Gemini call, bypassing the daily cache.
     * Only works if the merchant has enough real data.
     */
    fun retryPrediction(merchantId: String) {
        lastMerchantId = merchantId
        if (realMealsRescued < SurplusIqUiState.GEMINI_THRESHOLD) {
            _surplusIq.value = SurplusIqUiState.NewRestaurant(realMealsRescued)
            return
        }
        _surplusIq.value = SurplusIqUiState.Loading
        viewModelScope.launch {
            try {
                val (meals, reason) = com.reskyu.merchant.data.remote.GeminiApiService.predict(
                    salesHistory = _esgStats.value.weeklyData.map { it.toInt() }
                )
                _surplusIq.value = SurplusIqUiState.Success(
                    SurplusIqResult(predictedMeals = meals, reasoning = reason, confidence = 0.82f)
                )
            } catch (e: Exception) {
                _surplusIq.value = SurplusIqUiState.Error(friendlyError(e))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Decides what to put in [_surplusIq]:
     *
     *  - No real data (isRealData = false) or real meals < [GEMINI_THRESHOLD]
     *    → [NewRestaurant] — show "list X more times to unlock AI"
     *
     *  - Real meals ≥ [GEMINI_THRESHOLD]
     *    → Call [SurplusIqRepository.getPrediction] with the REAL sales history
     *      (1-per-day Firestore cache still applies)
     */
    private suspend fun updateSurplusIqState(
        merchantId:   String,
        salesHistory: List<Int>,
        isRealData:   Boolean
    ) {
        if (!isRealData || realMealsRescued < SurplusIqUiState.GEMINI_THRESHOLD) {
            _surplusIq.value = SurplusIqUiState.NewRestaurant(realMealsRescued)
            return
        }

        // Enough data — call Gemini (cached once per day)
        _surplusIq.value = SurplusIqUiState.Loading
        runCatching {
            SurplusIqRepository.getPrediction(
                uid          = merchantId,
                salesHistory = salesHistory
            )
        }.onSuccess { result ->
            _surplusIq.value = SurplusIqUiState.Success(result)
        }.onFailure {
            // Gemini unavailable but we have real data — compute local average
            // as a sensible generic suggestion instead of showing the "unlock" card
            _surplusIq.value = SurplusIqUiState.Success(localAveragePrediction(salesHistory))
        }
    }

    private fun friendlyError(e: Exception) = when {
        e.message?.contains("401") == true             -> "Invalid API key"
        e.message?.contains("429") == true             -> "Rate limit — retry in a minute"
        e.message?.contains("quota") == true           -> "Daily quota exceeded"
        e.message?.contains("Unable to resolve") == true -> "No internet connection"
        e.message?.contains("not set") == true         -> "Gemini API key missing"
        else                                            -> e.message ?: "Prediction unavailable"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion — demo / fallback data
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        /** Attractive demo ESG stats shown before real data loads. */
        fun demoEsgStats() = EsgStats(
            totalMealsRescued  = 45,
            co2SavedKg         = 112.5,
            foodWasteReducedKg = 18.0,
            totalRevenue       = 4671.0,
            weeklyData         = listOf(4f, 6f, 5f, 8f, 7f, 9f, 6f),
            weeklyRevenue      = listOf(316f, 594f, 595f, 1192f, 623f, 1161f, 654f),
            recoveryRateWeekly = listOf(80f, 86f, 83f, 89f, 88f, 90f, 86f),
            completedOrders    = 43,
            disputedOrders     = 2,
            topSellingItems    = mapOf(
                "Paneer Biryani"  to 12,
                "Veg Thali"       to 9,
                "Dal Makhani"     to 7,
                "Chole Bhature"   to 5,
                "Masala Dosa"     to 3
            )
        )

        fun localPrediction() = SurplusIqResult(
            predictedMeals = 7,
            reasoning      = "Stable trend — slight weekend dip expected",
            confidence     = 0.80f,
            cachedDate     = LocalDate.now().toString()
        )

        /**
         * Computes a local prediction when Gemini is unavailable.
         * Uses a weighted average (recent days weighted more) so trend matters.
         */
        fun localAveragePrediction(salesHistory: List<Int>): SurplusIqResult {
            val nonZero = salesHistory.filter { it > 0 }
            val predicted = if (nonZero.isEmpty()) {
                5  // safe default
            } else {
                // Weight recent days more: multiply by day index + 1
                val weighted = nonZero.takeLast(7)
                    .mapIndexed { i, v -> (i + 1) * v }
                    .sum()
                val weights  = (1..nonZero.takeLast(7).size).sum()
                (weighted.toDouble() / weights).toInt().coerceAtLeast(1)
            }
            return SurplusIqResult(
                predictedMeals = predicted,
                reasoning      = "Based on your 7-day sales trend",
                confidence     = 0.70f,
                cachedDate     = LocalDate.now().toString()
            )
        }
    }
}
