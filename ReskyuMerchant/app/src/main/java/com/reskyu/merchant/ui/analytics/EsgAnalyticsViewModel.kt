package com.reskyu.merchant.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.EsgStats
import com.reskyu.merchant.data.repository.EsgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EsgAnalyticsViewModel : ViewModel() {

    private val esgRepository = EsgRepository()

    private val _esgStats = MutableStateFlow(EsgStats())
    val esgStats: StateFlow<EsgStats> = _esgStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadStats(merchantId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _esgStats.value = esgRepository.getEsgStats(merchantId)
            } catch (e: Exception) {
                // TODO: error state
            } finally {
                _isLoading.value = false
            }
        }
    }
}
