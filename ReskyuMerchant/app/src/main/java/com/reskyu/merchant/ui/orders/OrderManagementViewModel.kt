package com.reskyu.merchant.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.ClaimTab
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.repository.MerchantClaimRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Result of scanning a customer QR code. */
sealed class QrScanResult {
    object Idle                          : QrScanResult()
    data class Success(val claimId: String, val heroItem: String) : QrScanResult()
    data class Error(val message: String)                         : QrScanResult()
}

class OrderManagementViewModel : ViewModel() {

    private val claimRepository = MerchantClaimRepository()

    // All claims from Firestore (unfiltered) — exposed for tab badge counts
    private val _allClaims = MutableStateFlow<List<MerchantClaim>>(emptyList())
    val allClaims: StateFlow<List<MerchantClaim>> = _allClaims

    private val _selectedTab = MutableStateFlow(ClaimTab.PENDING)
    val selectedTab: StateFlow<ClaimTab> = _selectedTab

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** QR scan result — reset to Idle after the dialog is dismissed. */
    private val _qrScanResult = MutableStateFlow<QrScanResult>(QrScanResult.Idle)
    val qrScanResult: StateFlow<QrScanResult> = _qrScanResult

    /**
     * Derived state: claims filtered to the currently selected tab.
     * Recomputes automatically whenever [_allClaims] or [_selectedTab] changes.
     */
    val filteredClaims: StateFlow<List<MerchantClaim>> =
        combine(_allClaims, _selectedTab) { claims, tab ->
            claims.filter { it.status == tab.toFirestoreStatus() }
        }.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun selectTab(tab: ClaimTab) { _selectedTab.value = tab }

    /**
     * Subscribes to real-time claim updates via Firestore snapshot listener.
     * New consumer orders appear automatically — no manual refresh needed.
     */
    fun loadClaims(merchantId: String) {
        viewModelScope.launch {
            claimRepository.observeClaimsForMerchant(merchantId)
                .onStart { _isLoading.value = true }
                .catch   { e -> _error.value = e.message; _isLoading.value = false }
                .collect { claims ->
                    _allClaims.value  = claims
                    _isLoading.value  = false
                    _error.value      = null
                }
        }
    }

    /** Marks a claim COMPLETED. Snapshot listener updates the UI automatically. */
    fun completeClaim(claimId: String) {
        viewModelScope.launch {
            try { claimRepository.completeClaim(claimId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    /** Marks a claim DISPUTED. Snapshot listener updates the UI automatically. */
    fun disputeClaim(claimId: String) {
        viewModelScope.launch {
            try { claimRepository.disputeClaim(claimId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    /** Resets QR scan result state back to Idle (call after dialog dismissed). */
    fun resetQrResult() { _qrScanResult.value = QrScanResult.Idle }

    /**
     * Processes a raw QR scan value and completes the matching order.
     *
     * QR payload formats accepted (tries both):
     *   1. JSON: {"orderId":"<id>","merchantId":"<uid>"}
     *   2. Pipe:  "<orderId>|<merchantId>"
     *
     * Validation:
     *  - merchantId in QR must match [currentMerchantId]
     *  - orderId must be a PENDING_PICKUP claim for this merchant
     */
    fun scanAndComplete(rawQr: String, currentMerchantId: String) {
        viewModelScope.launch {
            val (orderId, qrMerchantId) = parseQrPayload(rawQr)
                ?: run {
                    _qrScanResult.value = QrScanResult.Error(
                        "Invalid QR code. Please ask the customer to show their pickup pass."
                    )
                    return@launch
                }

            // ── Merchant ID check ──────────────────────────────────────────────
            if (qrMerchantId.isNotBlank() && qrMerchantId != currentMerchantId) {
                _qrScanResult.value = QrScanResult.Error(
                    "This order belongs to a different restaurant. Wrong QR code."
                )
                return@launch
            }

            // ── Find the claim in the live list ────────────────────────────────
            val claim = _allClaims.value.find { it.id == orderId }

            when {
                claim == null -> {
                    _qrScanResult.value = QrScanResult.Error(
                        "Order not found. It may have already been removed or never existed."
                    )
                }
                claim.status == "COMPLETED" -> {
                    _qrScanResult.value = QrScanResult.Error(
                        "This order has already been completed."
                    )
                }
                claim.status == "DISPUTED" -> {
                    _qrScanResult.value = QrScanResult.Error(
                        "This order is under dispute and cannot be completed via QR."
                    )
                }
                claim.status != "PENDING_PICKUP" -> {
                    _qrScanResult.value = QrScanResult.Error(
                        "Order status is '${claim.status}' — cannot complete."
                    )
                }
                else -> {
                    // ── All checks passed — complete the order ─────────────────
                    try {
                        claimRepository.completeClaim(orderId)
                        _qrScanResult.value = QrScanResult.Success(
                            claimId  = orderId,
                            heroItem = claim.heroItem.ifBlank { "Order" }
                        )
                    } catch (e: Exception) {
                        _qrScanResult.value = QrScanResult.Error(
                            "Failed to complete order: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Maps [ClaimTab] to the Firestore status string. */
    private fun ClaimTab.toFirestoreStatus(): String = when (this) {
        ClaimTab.PENDING   -> "PENDING_PICKUP"
        ClaimTab.COMPLETED -> "COMPLETED"
        ClaimTab.DISPUTED  -> "DISPUTED"
    }

    /**
     * Tries to parse QR payload into (claimId, merchantId) pair.
     *
     * JSON keys tried in order:
     *   1. "claimId"  (preferred — canonical claim document ID)
     *   2. "orderId"  (legacy fallback)
     *
     * Also accepts pipe-separated: "<claimId>|<merchantId>"
     * Or plain claimId string (no merchantId check).
     *
     * Returns null if no readable claim ID found.
     */
    private fun parseQrPayload(raw: String): Pair<String, String>? {
        // ── JSON format ─────────────────────────────────────────────────────
        runCatching {
            val json       = JSONObject(raw)
            val merchantId = json.optString("merchantId", "")
            // Prefer claimId; fall back to orderId for backward compat
            val claimId    = json.optString("claimId").takeIf { it.isNotBlank() }
                          ?: json.optString("orderId").takeIf { it.isNotBlank() }
            if (claimId != null) return Pair(claimId, merchantId)
        }
        // ── Pipe-separated: "<claimId>|<merchantId>" ────────────────────────
        if (raw.contains("|")) {
            val parts      = raw.split("|", limit = 2)
            val claimId    = parts[0].trim().takeIf { it.isNotBlank() }
            val merchantId = parts.getOrNull(1)?.trim() ?: ""
            if (claimId != null) return Pair(claimId, merchantId)
        }
        // ── Plain claim ID string (no merchantId check) ────────────────────
        val trimmed = raw.trim()
        if (trimmed.isNotBlank() && !trimmed.startsWith("{")) {
            return Pair(trimmed, "")
        }
        return null
    }
}

