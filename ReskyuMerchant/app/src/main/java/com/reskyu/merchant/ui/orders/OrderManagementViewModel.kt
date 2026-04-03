package com.reskyu.merchant.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.ClaimTab
import com.reskyu.merchant.data.model.MerchantClaim
import com.reskyu.merchant.data.repository.MerchantClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderManagementViewModel : ViewModel() {

    private val claimRepository = MerchantClaimRepository()

    private val _allClaims = MutableStateFlow<List<MerchantClaim>>(emptyList())

    private val _selectedTab = MutableStateFlow(ClaimTab.PENDING)
    val selectedTab: StateFlow<ClaimTab> = _selectedTab

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val filteredClaims: StateFlow<List<MerchantClaim>>
        get() = _allClaims // Filtered per tab in the UI via derivedStateOf or map

    fun selectTab(tab: ClaimTab) {
        _selectedTab.value = tab
    }

    fun loadClaims(merchantId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _allClaims.value = claimRepository.getClaimsForMerchant(merchantId)
            } catch (e: Exception) {
                // TODO: error state
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeClaim(claimId: String, merchantId: String) {
        viewModelScope.launch {
            claimRepository.completeClaim(claimId)
            loadClaims(merchantId)
        }
    }

    fun disputeClaim(claimId: String, merchantId: String) {
        viewModelScope.launch {
            claimRepository.disputeClaim(claimId)
            loadClaims(merchantId)
        }
    }
}
