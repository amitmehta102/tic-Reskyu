package com.reskyu.consumer.ui.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.model.PaymentState
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.ClaimRepository
import com.reskyu.consumer.data.repository.ListingRepository
import com.reskyu.consumer.data.repository.UserRepository
import com.reskyu.consumer.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ClaimViewModel
 *
 * Orchestrates the checkout flow:
 *  1. Load listing details for the order summary
 *  2. Initiate Razorpay payment (TODO: integrate SDK)
 *  3. On payment success → run Firestore transaction via [ClaimRepository]
 *  4. Update user ImpactStats via [UserRepository]
 *
 * ⚠️ Razorpay SDK requires an Activity reference — pass it from the Activity
 *    via a shared ViewModel or a callback mechanism (e.g., ActivityResultCaller).
 */
class ClaimViewModel : ViewModel() {

    private val listingRepository = ListingRepository()
    private val claimRepository = ClaimRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    /** Loads listing details needed for the order summary */
    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _listing.value = listingRepository.getListingById(listingId)
        }
    }

    /**
     * Starts the Razorpay payment flow and, on success, creates the Firestore claim.
     *
     * @param listing  The listing being claimed
     */
    fun initiatePayment(listing: Listing) {
        _paymentState.value = PaymentState.Processing

        // TODO: Initialize Razorpay SDK with RAZORPAY_KEY_ID from Constants
        // razorpay.open(options) — this needs an Activity reference
        // On payment success callback → call onPaymentSuccess(paymentId, listing)
        // On payment failure callback → _paymentState.value = PaymentState.Failed(reason)

        // Placeholder — remove when Razorpay is integrated
        viewModelScope.launch {
            try {
                // Simulate payment for development
                val fakePaymentId = "pay_DEV_${System.currentTimeMillis()}"
                onPaymentSuccess(fakePaymentId, listing)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Failed(e.message ?: "Payment failed")
            }
        }
    }

    /**
     * Called by the Razorpay success callback.
     * Creates the Claim in Firestore via atomic transaction and updates user stats.
     *
     * @param paymentId  Razorpay payment ID (format: pay_XXXXX)
     * @param listing    The listing that was claimed
     */
    fun onPaymentSuccess(paymentId: String, listing: Listing) {
        viewModelScope.launch {
            try {
                // Get UID — fall back to dev UID if auth is bypassed
                val uid = try {
                    authRepository.requireUid()
                } catch (e: Exception) {
                    "dev_user_${System.currentTimeMillis()}"
                }

                val claim = Claim(
                    userId = uid,
                    merchantId = listing.merchantId,
                    listingId = listing.id,
                    businessName = listing.businessName,
                    heroItem = listing.heroItem,
                    paymentId = paymentId,
                    amount = listing.discountedPrice,
                    originalPrice = listing.originalPrice,  // ← for savings display
                    timestamp = Timestamp.now(),
                    status = "PENDING_PICKUP"
                )

                val claimId = try {
                    claimRepository.createClaim(claim)
                } catch (e: Exception) {
                    // Firebase not configured — use paymentId as a dev claim ID
                    paymentId
                }

                try {
                    val amountSaved = listing.originalPrice - listing.discountedPrice
                    userRepository.updateImpactStats(uid, amountSaved)
                } catch (_: Exception) {
                    // Impact stats update failure is non-critical; proceed
                }

                _paymentState.value = PaymentState.Success(claimId)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Failed(e.message ?: "Failed to confirm claim")
            }
        }
    }

    fun onPaymentFailed(reason: String) {
        _paymentState.value = PaymentState.Failed(reason)
    }
}
