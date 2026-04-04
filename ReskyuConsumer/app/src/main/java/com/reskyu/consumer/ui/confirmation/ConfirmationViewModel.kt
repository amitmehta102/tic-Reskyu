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

class ConfirmationViewModel : ViewModel() {

    private val claimRepository = ClaimRepository()

    private val _ticketState = MutableStateFlow<TicketUiState?>(null)
    val ticketState: StateFlow<TicketUiState?> = _ticketState.asStateFlow()

    fun loadTicket(claimId: String) {
        viewModelScope.launch {
            try {
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
                    _ticketState.value = devTicket(claimId)
                }
            } catch (e: Exception) {
                _ticketState.value = devTicket(claimId)
            }
        }
    }

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
