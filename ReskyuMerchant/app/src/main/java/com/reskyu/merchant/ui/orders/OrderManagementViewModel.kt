package com.reskyu.merchant.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.ClaimTab
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.repository.MerchantClaimRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    /**
     * Derived state: claims filtered to the currently selected tab.
     * Recomputes automatically whenever [_allClaims] or [_selectedTab] changes.
     */
    val filteredClaims: StateFlow<List<MerchantClaim>> =
        combine(_allClaims, _selectedTab) { claims, tab ->
            claims.filter { it.status == tab.toFirestoreStatus() }
        }.stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.Lazily,
            initialValue   = emptyList()
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Maps [ClaimTab] to the Firestore status string. */
    private fun ClaimTab.toFirestoreStatus(): String = when (this) {
        ClaimTab.PENDING   -> "PENDING_PICKUP"
        ClaimTab.COMPLETED -> "COMPLETED"
        ClaimTab.DISPUTED  -> "DISPUTED"
    }
}
