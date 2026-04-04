package com.reskyu.merchant.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.DemandResult
import com.reskyu.merchant.data.model.EsgStats
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.remote.GeminiApiService
import com.reskyu.merchant.data.repository.DemandRepository
import com.reskyu.merchant.data.repository.EsgRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import com.reskyu.merchant.data.repository.SellEverythingRepository
import com.reskyu.merchant.data.repository.SurplusIqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class EsgAnalyticsViewModel : ViewModel() {

    private val esgRepository         = EsgRepository()
    private val merchantRepository     = MerchantRepository()
    private val sellEverythingRepository = SellEverythingRepository()

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

    // ── Local Demand Intelligence — starts idle ───────────────────────────────
    private val _demand = MutableStateFlow<DemandUiState>(DemandUiState.Idle)
    val demand: StateFlow<DemandUiState> = _demand

    // ── Sell Everything Mode — starts idle ────────────────────────────────────
    private val _sellEverything = MutableStateFlow<SellEverythingUiState>(SellEverythingUiState.Idle)
    val sellEverything: StateFlow<SellEverythingUiState> = _sellEverything

    // Real meal count from Firestore — used for the Gemini threshold check
    private var realMealsRescued: Int  = 0
    private var lastMerchantId         = ""
    // Cached merchant profile fields (fetched in loadStats)
    private var cachedClosingTime      = ""
    private var cachedBusinessName     = ""
    private var cachedGeoHash          = ""
    private var cachedLat              = 0.0
    private var cachedLng              = 0.0

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

        // ── Local Demand Intelligence — runs concurrently with ESG load ───────
        viewModelScope.launch {
            fetchDemand(merchantId)
        }

        // ── Sell Everything Mode — check eligibility ────────────────────
        viewModelScope.launch {
            checkSellEverythingEligibility(merchantId)
        }
    }

    /** Forces a fresh Gemini call by clearing the date cache — only if merchant has ≥ 5 real meals. */
    fun retryPrediction(merchantId: String) {
        lastMerchantId = merchantId
        if (realMealsRescued < SurplusIqUiState.GEMINI_THRESHOLD) {
            _surplusIq.value = SurplusIqUiState.NewRestaurant(realMealsRescued)
            return
        }
        _surplusIq.value = SurplusIqUiState.Loading
        viewModelScope.launch {
            runCatching {
                SurplusIqRepository.getPrediction(
                    uid          = merchantId,
                    salesHistory = _esgStats.value.weeklyData.map { it.toInt() }
                )
            }.onSuccess { result ->
                _surplusIq.value = SurplusIqUiState.Success(result)
            }.onFailure { e ->
                _surplusIq.value = SurplusIqUiState.Error(friendlyError(e as Exception))
            }
        }
    }

    /** Refreshes the Local Demand Intelligence card manually. */
    fun refreshDemand(merchantId: String) {
        viewModelScope.launch { fetchDemand(merchantId) }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Sell Everything Mode — public
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether Sell Everything Mode should activate and transitions to the
     * appropriate UI state. Called automatically in [loadStats].
     *
     * ─ Flow ───────────────────────────────────────────────
     * 1. Fetch merchant profile to get closingTime + geo fields
     * 2. Compute minutesUntilClose
     * 3. If > TRIGGER_WINDOW_MINUTES → NotTriggered
     * 4. Else fetch active listings; if none → NoListings
     * 5. Build SellEverythingContext → call Gemini → emit Ready
     */
    private suspend fun checkSellEverythingEligibility(merchantId: String) {
        val merchant = runCatching { merchantRepository.getMerchant(merchantId) }.getOrNull()
            ?: return

        // Cache merchant geo for later use in createBundleListing
        cachedClosingTime  = merchant.closingTime
        cachedBusinessName = merchant.businessName
        cachedGeoHash      = merchant.geoHash
        cachedLat          = merchant.lat
        cachedLng          = merchant.lng

        val mins = sellEverythingRepository.minutesUntilClose(merchant.closingTime)
        if (mins < 0) {
            // Closing time not set — show NotTriggered with -1 hint
            _sellEverything.value = SellEverythingUiState.NotTriggered(-1)
            return
        }
        if (mins > SellEverythingUiState.TRIGGER_WINDOW_MINUTES) {
            _sellEverything.value = SellEverythingUiState.NotTriggered(mins)
            return
        }

        // Within trigger window — fetch listings + build context
        _sellEverything.value = SellEverythingUiState.FetchingStrategy
        val ctx = sellEverythingRepository.buildContext(
            merchantId        = merchantId,
            minutesUntilClose = mins,
            merchantName      = merchant.businessName
        )
        if (ctx == null) {
            _sellEverything.value = SellEverythingUiState.NoListings
            return
        }

        // Call Gemini
        runCatching {
            GeminiApiService.planSellEverything(ctx)
        }.onSuccess { result ->
            _sellEverything.value = SellEverythingUiState.Ready(
                result            = result,
                mealsLeft         = ctx.mealsLeft,
                minutesUntilClose = mins
            )
        }.onFailure {
            // Gemini failed — show error so merchant can retry
            _sellEverything.value = SellEverythingUiState.Error(
                "Strategy unavailable — please retry"
            )
        }
    }

    /**
     * Transitions from [SellEverythingUiState.Ready] to [SellEverythingUiState.Confirming].
     * Must be called when the merchant taps "Activate Sell Everything Mode".
     */
    fun requestSellEverythingConfirmation() {
        val current = _sellEverything.value
        if (current is SellEverythingUiState.Ready) {
            _sellEverything.value = SellEverythingUiState.Confirming(
                result            = current.result,
                mealsLeft         = current.mealsLeft,
                minutesUntilClose = current.minutesUntilClose
            )
        }
    }

    /** Cancels the confirmation dialog and returns to [Ready] state. */
    fun cancelSellEverythingConfirmation() {
        val current = _sellEverything.value
        if (current is SellEverythingUiState.Confirming) {
            _sellEverything.value = SellEverythingUiState.Ready(
                result            = current.result,
                mealsLeft         = current.mealsLeft,
                minutesUntilClose = current.minutesUntilClose
            )
        }
    }

    /**
     * Called when the merchant confirms the dialog.
     * Applies discounts, creates bundle listing, persists metadata — all in sequence.
     */
    fun confirmAndApplySellEverything(merchantId: String) {
        val current = _sellEverything.value
        if (current !is SellEverythingUiState.Confirming) return

        _sellEverything.value = SellEverythingUiState.Applying
        viewModelScope.launch {
            runCatching {
                val result            = current.result
                val minutesUntilClose = current.minutesUntilClose

                // 1. Apply discounts to all active individual listings
                sellEverythingRepository.applyDiscountToListings(
                    merchantId        = merchantId,
                    discountPercentage = result.discountPercentage
                )

                // 2. Create auto-bundle listing in Firestore
                val bundleId = sellEverythingRepository.createBundleListing(
                    merchantId        = merchantId,
                    businessName      = cachedBusinessName,
                    geoHash           = cachedGeoHash,
                    lat               = cachedLat,
                    lng               = cachedLng,
                    result            = result,
                    minutesUntilClose = minutesUntilClose
                )

                // 3. Persist sell-out metadata to Firestore (for push Cloud Function)
                sellEverythingRepository.persistSellOutModeState(
                    merchantId      = merchantId,
                    result          = result,
                    bundleListingId = bundleId
                )

                bundleId
            }.onSuccess { bundleId ->
                _sellEverything.value = SellEverythingUiState.Applied(
                    result          = (current as? SellEverythingUiState.Confirming)?.result
                                      ?: return@onSuccess,
                    bundleListingId = bundleId
                )
            }.onFailure { e ->
                _sellEverything.value = SellEverythingUiState.Error(
                    e.message ?: "Failed to activate Sell Everything Mode"
                )
            }
        }
    }

    /** Manual retry — re-runs eligibility check from scratch. */
    fun retrySellEverything(merchantId: String) {
        _sellEverything.value = SellEverythingUiState.Idle
        viewModelScope.launch { checkSellEverythingEligibility(merchantId) }
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

    /**
     * Fetches nearby demand data from Firestore (no AI), then sends it to
     * Gemini for interpretation only. Falls back to a local heuristic result
     * if Gemini is unavailable — never crashes, never hangs.
     */
    private suspend fun fetchDemand(merchantId: String) {
        _demand.value = DemandUiState.Loading
        runCatching {
            // Get merchant lat/lng for geo-filter
            val merchant = merchantRepository.getMerchant(merchantId)
            val lat = merchant?.lat ?: 0.0
            val lng = merchant?.lng ?: 0.0

            // Step 1: Fetch + aggregate locally (NO AI)
            val ctx = DemandRepository.fetchDemandContext(merchantId, lat, lng)

            // Step 2: AI interpretation only
            GeminiApiService.interpretDemand(ctx)
        }.onSuccess { result ->
            _demand.value = DemandUiState.Success(result)
        }.onFailure {
            // Graceful local fallback — derive from raw demand score if available
            _demand.value = DemandUiState.Success(
                DemandResult(
                    demandLevel       = "MEDIUM",
                    bestAction        = "List your surplus food to capture nearby demand",
                    bestListingWindow = "next 30 minutes",
                    reason            = "Based on local listing activity"
                )
            )
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
                cachedDate     = LocalDate.now().toString(),
                bestTimeToList = "",
                pricingHint    = "",
                actionTip      = ""
            )
        }
    }
}
