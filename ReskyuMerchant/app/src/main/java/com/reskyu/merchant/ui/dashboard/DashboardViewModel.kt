package com.reskyu.merchant.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.DashboardStats
import com.reskyu.merchant.data.model.SurplusIqResult
import com.reskyu.merchant.data.repository.EsgRepository
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantClaimRepository
import com.reskyu.merchant.data.repository.SurplusIqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val authRepository = MerchantAuthRepository()
    private val claimRepository = MerchantClaimRepository()
    private val esgRepository = EsgRepository()
    private val surplusIqRepository = SurplusIqRepository()

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _surplusIqResult = MutableStateFlow<SurplusIqResult?>(null)
    val surplusIqResult: StateFlow<SurplusIqResult?> = _surplusIqResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadDashboard(merchantId: String, lastPredDate: String, lastPredMeals: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val claims = claimRepository.getClaimsForMerchant(merchantId)
                val pending = claims.count { it.status == "PENDING_PICKUP" }
                val completed = claims.count { it.status == "COMPLETED" }
                val revenue = claims.filter { it.status == "COMPLETED" }.sumOf { it.amount }

                _stats.value = DashboardStats(
                    totalMealsRescued = completed,
                    totalRevenue = revenue,
                    pendingClaims = pending
                )
            } catch (e: Throwable) {
                // Silently degrade — UI already shows zero/empty state
            }

            // SurplusIQ is guarded separately: GeminiApiService throws NotImplementedError
            // (which is a Kotlin Error, not Exception) so it must be caught as Throwable
            try {
                val prediction = surplusIqRepository.getPrediction(
                    uid = merchantId,
                    lastPredDate = lastPredDate,
                    lastPredMeals = lastPredMeals,
                    salesHistory = emptyList()
                )
                _surplusIqResult.value = prediction
            } catch (e: Throwable) {
                // SurplusIQ not yet implemented — banner stays hidden
            } finally {
                _isLoading.value = false
            }
        }
    }
}
