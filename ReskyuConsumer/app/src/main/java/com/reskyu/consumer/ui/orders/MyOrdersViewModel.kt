package com.reskyu.consumer.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.ClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * MyOrdersViewModel
 *
 * Subscribes to a real-time Firestore snapshot of the current user's claims.
 * Status changes (PENDING_PICKUP → COMPLETED) appear live without pull-to-refresh.
 *
 * Tab logic:
 *  "Current" tab → claims with status PENDING_PICKUP
 *  "Past" tab    → all other statuses (COMPLETED, REFUNDED, DISPUTED, etc.)
 */
class MyOrdersViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()
    private val authRepository  = AuthRepository()

    private val _allClaims = MutableStateFlow<List<Claim>>(emptyList())
    val allClaims: StateFlow<List<Claim>> = _allClaims.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { subscribeToOrders() }

    private fun subscribeToOrders() {
        val uid = try { authRepository.requireUid() } catch (_: Exception) { return }

        viewModelScope.launch {
            _isLoading.value = true
            claimRepository
                .observeClaimsForUser(uid)
                .catch { _isLoading.value = false }
                .collect { claims ->
                    _isLoading.value = false
                    _allClaims.value = claims
                }
        }
    }

    fun refresh() = subscribeToOrders()

    /**
     * Saves a 1–5 star rating for a completed order.
     * Writes to the claim doc and updates the merchant's ratingSum/ratingCount atomically.
     * The snapshot listener auto-refreshes the UI — no manual state update needed.
     */
    fun submitRating(claimId: String, merchantId: String, stars: Int) {
        viewModelScope.launch {
            try {
                claimRepository.submitRating(claimId, merchantId, stars)
            } catch (_: Exception) {
                // Already rated or Firestore error — fail silently
            }
        }
    }
}
