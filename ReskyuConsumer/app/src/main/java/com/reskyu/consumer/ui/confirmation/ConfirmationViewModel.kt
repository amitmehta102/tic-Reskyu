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
import java.util.concurrent.TimeUnit

/**
 * ConfirmationViewModel
 *
 * Loads the newly-created claim from Firestore and maps it to [TicketUiState].
 * Falls back to a dev stub if Firestore fails (e.g., during offline/dev mode).
 *
 * The claimId passed here is either:
 *  - A real Firestore document ID (prod)
 *  - A "pay_DEV_..." string from the dev payment simulation (dev mode)
 */
class ConfirmationViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()

    private val _ticketState = MutableStateFlow<TicketUiState?>(null)
    val ticketState: StateFlow<TicketUiState?> = _ticketState.asStateFlow()

    fun loadTicket(claimId: String) {
        viewModelScope.launch {
            try {
                // Try real Firestore fetch first
                val claim = claimRepository.getClaimById(claimId)
                if (claim != null) {
                    _ticketState.value = TicketUiState(
                        claimId = claim.id,
                        businessName = claim.businessName,
                        heroItem = claim.heroItem,
                        amount = claim.amount,
                        paymentId = claim.paymentId,
                        pickupByTime = formatPickupTime(
                            if (claim.pickupDeadlineMs > 0) claim.pickupDeadlineMs
                            else claim.timestamp.toDate().time + TimeUnit.HOURS.toMillis(2)
                        )
                    )
                } else {
                    // Dev fallback — claimId is the payment ID from the simulated checkout
                    _ticketState.value = devTicket(claimId)
                }
            } catch (e: Exception) {
                // Firebase not configured / offline — show a dev ticket
                _ticketState.value = devTicket(claimId)
            }
        }
    }

    /** Dev-mode placeholder ticket shown when Firestore isn't reachable */
    private fun devTicket(paymentId: String) = TicketUiState(
        claimId = paymentId,
        businessName = "Dev Bakery Co.",
        heroItem = "Assorted Pastries Box (Dev)",
        amount = 149.0,
        paymentId = paymentId,
        pickupByTime = formatPickupTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2))
    )

    private fun formatPickupTime(timestampMs: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "by ${sdf.format(Date(timestampMs))}"
    }
}
