package com.reskyu.consumer.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.model.OrderTab
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.ClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * MyOrdersViewModel
 *
 * Loads all claims for the current user and filters them by [OrderTab].
 * Falls back to dev sample data when Firebase Auth is not configured.
 *
 * Tab → Firestore status mapping:
 *  - UPCOMING   → "PENDING_PICKUP"
 *  - COMPLETED  → "COMPLETED"
 *  - EXPIRED    → anything else (EXPIRED, DISPUTED, etc.)
 */
class MyOrdersViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()
    private val authRepository = AuthRepository()

    private val _allClaims = MutableStateFlow<List<Claim>>(emptyList())
    val allClaims: StateFlow<List<Claim>> = _allClaims.asStateFlow()

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
            } catch (e: Exception) {
                // Firebase auth not configured — show dev sample data
                _allClaims.value = devSampleClaims()
            } finally {
                applyFilter(_selectedTab.value)
                _isLoading.value = false
            }
        }
    }

    fun selectTab(tab: OrderTab) {
        _selectedTab.value = tab
        applyFilter(tab)
    }

    fun refresh() = loadOrders()

    private fun applyFilter(tab: OrderTab) {
        _filteredClaims.value = when (tab) {
            OrderTab.UPCOMING  -> _allClaims.value.filter { it.status == "PENDING_PICKUP" }
            OrderTab.COMPLETED -> _allClaims.value.filter { it.status == "COMPLETED" }
            OrderTab.EXPIRED   -> _allClaims.value.filter {
                it.status != "PENDING_PICKUP" && it.status != "COMPLETED"
            }
        }
    }

    /** Sample claims shown in dev mode before Firebase is configured */
    private fun devSampleClaims() = listOf(
        Claim(
            id = "dev_claim_1",
            userId = "dev",
            businessName = "The Bread Basket",
            heroItem = "Assorted Pastry Box",
            paymentId = "pay_DEV_001",
            amount = 99.0,
            originalPrice = 280.0,
            timestamp = Timestamp(System.currentTimeMillis() / 1000 - TimeUnit.HOURS.toSeconds(1), 0),
            status = "PENDING_PICKUP"
        ),
        Claim(
            id = "dev_claim_2",
            userId = "dev",
            businessName = "Green Leaf Café",
            heroItem = "Veg Thali Combo",
            paymentId = "pay_DEV_002",
            amount = 79.0,
            originalPrice = 200.0,
            timestamp = Timestamp(System.currentTimeMillis() / 1000 - TimeUnit.DAYS.toSeconds(1), 0),
            status = "COMPLETED"
        ),
        Claim(
            id = "dev_claim_3",
            userId = "dev",
            businessName = "Spice Garden",
            heroItem = "Biryani & Curry Pack",
            paymentId = "pay_DEV_003",
            amount = 149.0,
            originalPrice = 380.0,
            timestamp = Timestamp(System.currentTimeMillis() / 1000 - TimeUnit.DAYS.toSeconds(3), 0),
            status = "EXPIRED"
        )
    )
}
