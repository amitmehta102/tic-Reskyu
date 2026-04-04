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

    // ── ESG stats — starts EMPTY; only populated with real Firestore data ──────
    private val _esgStats = MutableStateFlow(EsgStats())
    val esgStats: StateFlow<EsgStats> = _esgStats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * True when Firestore returned no completed claims for this merchant.
     * When true the screen shows the "New Restaurant" empty state instead of all charts.
     */
    private val _isNewRestaurant = MutableStateFlow(false)
    val isNewRestaurant: StateFlow<Boolean> = _isNewRestaurant

    // ── SurplusIQ — starts as NewRestaurant(0) until real data is known ─────
    private val _surplusIq = MutableStateFlow<SurplusIqUiState>(
        SurplusIqUiState.NewRestaurant(mealsRescued = 0)
    )
    val surplusIq: StateFlow<SurplusIqUiState> = _surplusIq

    // Real meal count from Firestore — used for the Gemini threshold check
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
            // Load real ESG stats from Firestore
            val realStats = runCatching { esgRepository.getEsgStats(merchantId) }.getOrNull()

            if (realStats != null && realStats.totalMealsRescued > 0) {
                // Merchant has real completed orders — show actual data
                _esgStats.value     = realStats
                realMealsRescued    = realStats.totalMealsRescued
                _isNewRestaurant.value = false
            } else {
                // No completed claims yet — new restaurant empty state
                _esgStats.value     = EsgStats()
                realMealsRescued    = 0
                _isNewRestaurant.value = true
            }
            _isLoading.value = false

            // SurplusIQ decision (respects the same real-data threshold)
            updateSurplusIqState(
                merchantId   = merchantId,
                salesHistory = _esgStats.value.weeklyData.map { it.toInt() },
                isRealData   = realMealsRescued > 0
            )
        }
    }

    /** Forces a fresh Gemini call — only if merchant has ≥ 5 real meals. */
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

    private suspend fun updateSurplusIqState(
        merchantId:   String,
        salesHistory: List<Int>,
        isRealData:   Boolean
    ) {
        if (!isRealData || realMealsRescued < SurplusIqUiState.GEMINI_THRESHOLD) {
            _surplusIq.value = SurplusIqUiState.NewRestaurant(realMealsRescued)
            return
        }
        _surplusIq.value = SurplusIqUiState.Loading
        runCatching {
            SurplusIqRepository.getPrediction(uid = merchantId, salesHistory = salesHistory)
        }.onSuccess { result ->
            _surplusIq.value = SurplusIqUiState.Success(result)
        }.onFailure {
            // Gemini failed but have real data → local weighted-average fallback
            _surplusIq.value = SurplusIqUiState.Success(localAveragePrediction(salesHistory))
        }
    }

    private fun friendlyError(e: Exception) = when {
        e.message?.contains("401") == true              -> "Invalid API key"
        e.message?.contains("429") == true              -> "Rate limit — retry in a minute"
        e.message?.contains("quota") == true            -> "Daily quota exceeded"
        e.message?.contains("Unable to resolve") == true-> "No internet connection"
        e.message?.contains("not set") == true          -> "Gemini API key missing"
        else                                             -> e.message ?: "Prediction unavailable"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        fun localPrediction() = SurplusIqResult(
            predictedMeals = 7,
            reasoning      = "Stable trend — slight weekend dip expected",
            confidence     = 0.80f,
            cachedDate     = LocalDate.now().toString()
        )

        /** Weighted-average local prediction when Gemini is unavailable. */
        fun localAveragePrediction(salesHistory: List<Int>): SurplusIqResult {
            val nonZero = salesHistory.filter { it > 0 }
            val predicted = if (nonZero.isEmpty()) {
                5
            } else {
                val weighted = nonZero.takeLast(7).mapIndexed { i, v -> (i + 1) * v }.sum()
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
