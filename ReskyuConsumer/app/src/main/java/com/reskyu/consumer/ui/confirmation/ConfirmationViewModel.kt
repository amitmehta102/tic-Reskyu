package com.reskyu.consumer.ui.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.TicketUiState
import com.reskyu.consumer.data.repository.ClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ConfirmationViewModel
 *
 * Loads the newly created claim document and maps it to a [TicketUiState]
 * for display on the [ConfirmationScreen].
 *
 * In most cases the data is already available in-memory from [ClaimViewModel]
 * (since we just created it). Loading from Firestore here is a safety fallback
 * for cases like deep-link navigation to the confirmation screen.
 */
class ConfirmationViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()

    private val _ticketState = MutableStateFlow<TicketUiState?>(null)
    val ticketState: StateFlow<TicketUiState?> = _ticketState.asStateFlow()

    /**
     * Loads claim data and maps it to [TicketUiState].
     *
     * @param claimId  Firestore document ID of the created claim
     */
    fun loadTicket(claimId: String) {
        viewModelScope.launch {
            try {
                // TODO: Add a getClaimById method to ClaimRepository
                // val claim = claimRepository.getClaimById(claimId)
                // For now, build a placeholder ticket
                _ticketState.value = TicketUiState(
                    claimId = claimId,
                    businessName = "Loading...",
                    heroItem = "...",
                    amount = 0.0,
                    paymentId = claimId,
                    pickupByTime = formatPickupTime(System.currentTimeMillis() + 2 * 60 * 60 * 1000) // +2 hours
                )
                // TODO: Replace placeholder with real claim data from repository
            } catch (e: Exception) {
                // Silently fail — the ticket will remain in loading state
            }
        }
    }

    /**
     * Formats a Unix timestamp as a human-readable pickup deadline.
     * Example: 1684350000000 → "by 10:00 PM"
     */
    private fun formatPickupTime(timestampMs: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "by ${sdf.format(Date(timestampMs))}"
    }
}
