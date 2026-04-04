package com.reskyu.merchant.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.EsgStats
import com.reskyu.merchant.data.repository.EsgRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import com.reskyu.merchant.data.repository.SurplusIqRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EsgAnalyticsViewModel : ViewModel() {

    private val esgRepository       = EsgRepository()
    private val merchantRepository  = MerchantRepository()
    private val surplusIqRepository = SurplusIqRepository()

    // ── ESG stats ─────────────────────────────────────────────────────────────

    private val _esgStats  = MutableStateFlow(EsgStats())
    val esgStats: StateFlow<EsgStats> = _esgStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── SurplusIQ ─────────────────────────────────────────────────────────────

    private val _surplusIq = MutableStateFlow<SurplusIqUiState>(SurplusIqUiState.Loading)
    val surplusIq: StateFlow<SurplusIqUiState> = _surplusIq

    // ── Last merchantId to enable retry ───────────────────────────────────────
    private var lastMerchantId: String = ""

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Loads ESG stats and SurplusIQ prediction in parallel.
     * Merchant profile is fetched to obtain Firestore-cached prediction date/meals.
     */
    fun loadStats(merchantId: String) {
        lastMerchantId = merchantId
        _isLoading.value = true

        viewModelScope.launch {
            // ESG stats and merchant profile load in parallel
            val statsDeferred    = async { runCatching { esgRepository.getEsgStats(merchantId) }.getOrNull() }
            val merchantDeferred = async { runCatching { merchantRepository.getMerchant(merchantId) }.getOrNull() }

            val stats    = statsDeferred.await()
            val merchant = merchantDeferred.await()

            if (stats != null) _esgStats.value = stats
            _isLoading.value = false

            // SurplusIQ uses the Firestore cache keys from the merchant profile
            fetchSurplusIq(
                merchantId    = merchantId,
                lastPredDate  = merchant?.lastPredictionDate  ?: "",
                lastPredMeals = merchant?.lastPredictionMeals ?: 0,
                salesHistory  = stats?.weeklyData?.map { it.toInt() } ?: emptyList()
            )
        }
    }

    fun retryPrediction(merchantId: String) {
        lastMerchantId = merchantId
        viewModelScope.launch {
            val merchant = runCatching { merchantRepository.getMerchant(merchantId) }.getOrNull()
            fetchSurplusIq(
                merchantId    = merchantId,
                lastPredDate  = merchant?.lastPredictionDate  ?: "",
                lastPredMeals = merchant?.lastPredictionMeals ?: 0,
                salesHistory  = _esgStats.value.weeklyData.map { it.toInt() }
            )
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun fetchSurplusIq(
        merchantId:    String,
        lastPredDate:  String,
        lastPredMeals: Int,
        salesHistory:  List<Int>
    ) {
        _surplusIq.value = SurplusIqUiState.Loading
        try {
            val result = surplusIqRepository.getPrediction(
                uid           = merchantId,
                lastPredDate  = lastPredDate,
                lastPredMeals = lastPredMeals,
                salesHistory  = salesHistory.ifEmpty { listOf(5, 3, 7, 4, 6, 8, 5) }
            )
            _surplusIq.value = SurplusIqUiState.Success(result)
        } catch (e: Throwable) {
            _surplusIq.value = SurplusIqUiState.Error(
                e.message ?: "SurplusIQ prediction unavailable"
            )
        }
    }
}
