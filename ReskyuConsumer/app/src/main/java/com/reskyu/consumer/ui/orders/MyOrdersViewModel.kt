package com.reskyu.consumer.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.model.OrderTab
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.ClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MyOrdersViewModel
 *
 * Loads all claims for the current user and filters them by [OrderTab].
 * Filtering is done client-side after a single Firestore fetch.
 *
 * Tab → Firestore status mapping:
 *  - UPCOMING   → status == "PENDING_PICKUP"
 *  - COMPLETED  → status == "COMPLETED"
 *  - EXPIRED    → status == "PENDING_PICKUP" AND listing has expired
 *                 (simplification: treat all non-completed/non-pending as expired)
 */
class MyOrdersViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()
    private val authRepository = AuthRepository()

    private val _allClaims = MutableStateFlow<List<Claim>>(emptyList())

    private val _selectedTab = MutableStateFlow(OrderTab.UPCOMING)
    val selectedTab: StateFlow<OrderTab> = _selectedTab.asStateFlow()

    private val _filteredClaims = MutableStateFlow<List<Claim>>(emptyList())
    val filteredClaims: StateFlow<List<Claim>> = _filteredClaims.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadOrders() }

    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = authRepository.requireUid()
                _allClaims.value = claimRepository.getClaimsForUser(uid)
                applyFilter(_selectedTab.value)
            } catch (e: Exception) {
                // TODO: Expose error state to UI
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the selected tab and re-filters the claims list.
     * @param tab  The [OrderTab] selected by the user
     */
    fun selectTab(tab: OrderTab) {
        _selectedTab.value = tab
        applyFilter(tab)
    }

    private fun applyFilter(tab: OrderTab) {
        _filteredClaims.value = when (tab) {
            OrderTab.UPCOMING  -> _allClaims.value.filter { it.status == "PENDING_PICKUP" }
            OrderTab.COMPLETED -> _allClaims.value.filter { it.status == "COMPLETED" }
            OrderTab.EXPIRED   -> _allClaims.value.filter {
                it.status != "PENDING_PICKUP" && it.status != "COMPLETED"
            }
        }
    }
}
