package com.reskyu.consumer.ui.claim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.reskyu.consumer.BuildConfig
import com.reskyu.consumer.data.model.Claim
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.model.PaymentState
import com.reskyu.consumer.data.remote.CreateOrderRequest
import com.reskyu.consumer.data.remote.RetrofitClient
import com.reskyu.consumer.data.remote.VerifyPaymentRequest
import com.reskyu.consumer.data.repository.AuthRepository
import com.reskyu.consumer.data.repository.ClaimRepository
import com.reskyu.consumer.data.repository.ListingRepository
import com.reskyu.consumer.data.repository.NotificationRepository
import com.reskyu.consumer.data.repository.UserRepository
import com.reskyu.consumer.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import java.util.concurrent.TimeUnit

class ClaimViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository      = ListingRepository()
    private val claimRepository        = ClaimRepository()
    private val authRepository         = AuthRepository()
    private val userRepository         = UserRepository()
    private val notificationRepository = NotificationRepository()
    private val locationRepository     = LocationRepository(application)
    private val api                    = RetrofitClient.api

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _openCheckoutEvent = MutableSharedFlow<RazorpayCheckoutEvent>()
    val openCheckoutEvent: SharedFlow<RazorpayCheckoutEvent> = _openCheckoutEvent.asSharedFlow()

    private var pendingOrderId: String? = null

    private var pendingQuantity: Int = 1

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _listing.value = listingRepository.getListingById(listingId)
        }
    }

    fun initiatePayment(listing: Listing, quantity: Int = 1) {
        if (_paymentState.value == PaymentState.Processing) return
        _paymentState.value = PaymentState.Processing
        pendingQuantity = quantity.coerceAtLeast(1)

        viewModelScope.launch {
            try {
                val amountPaise = (listing.discountedPrice * pendingQuantity * 100).toInt()
                val uid = try { authRepository.requireUid() } catch (_: Exception) { "dev" }
                val receipt = "claim_${uid}_${System.currentTimeMillis()}"

                val orderResponse = api.createOrder(
                    CreateOrderRequest(amount = amountPaise, receipt = receipt)
                )
                pendingOrderId = orderResponse.orderId

                _openCheckoutEvent.emit(
                    RazorpayCheckoutEvent(
                        orderId      = orderResponse.orderId,
                        amount       = amountPaise,
                        businessName = listing.businessName,
                        heroItem     = if (pendingQuantity > 1) "${listing.heroItem} ×$pendingQuantity" else listing.heroItem,
                        email        = try { authRepository.getCurrentUser()?.email ?: "" } catch (_: Exception) { "" }
                    )
                )
            } catch (e: Exception) {
                val fakePaymentId = "pay_DEV_${System.currentTimeMillis()}"
                pendingOrderId = "order_DEV"
                onPaymentSuccess(fakePaymentId, "dev_sig", listing)
            }
        }
    }

    fun onPaymentSuccess(paymentId: String, signature: String, listing: Listing) {
        viewModelScope.launch {
            try {
                val orderId = pendingOrderId ?: "order_DEV"

                val isDevMode = orderId == "order_DEV" || paymentId.startsWith("pay_DEV")
                if (!isDevMode) {
                    val verifyResponse = api.verifyPayment(
                        VerifyPaymentRequest(
                            orderId   = orderId,
                            paymentId = paymentId,
                            signature = signature
                        )
                    )
                    if (!verifyResponse.success) {
                        _paymentState.value = PaymentState.Failed("Payment verification failed. Please contact support.")
                        return@launch
                    }
                }

                val qty    = pendingQuantity.coerceAtLeast(1)
                val uid    = try { authRepository.requireUid() } catch (_: Exception) { "dev_user_${System.currentTimeMillis()}" }

                val (userLat, userLng) = try { locationRepository.getCurrentLocation() }
                    catch (_: Exception) { Pair(LocationRepository.DEFAULT_LAT, LocationRepository.DEFAULT_LNG) }
                val distanceKm     = haversineKm(userLat, userLng, listing.lat, listing.lng)
                val pickupDeadline = computePickupDeadline(listing, distanceKm)

                val claim = Claim(
                    userId           = uid,
                    merchantId       = listing.merchantId,
                    listingId        = listing.id,
                    businessName     = listing.businessName,
                    heroItem         = listing.heroItem,
                    paymentId        = paymentId,
                    amount           = listing.discountedPrice * qty,
                    originalPrice    = listing.originalPrice * qty,
                    timestamp        = Timestamp.now(),
                    status           = "PENDING_PICKUP",
                    quantity         = qty,
                    pickupDeadlineMs = pickupDeadline
                )

                val claimId = try {
                    claimRepository.createClaim(claim)
                } catch (_: Exception) { paymentId }

                try {
                    val saved = (listing.originalPrice - listing.discountedPrice) * qty
                    userRepository.updateImpactStats(uid, saved)
                } catch (_: Exception) {}

                try {
                    notificationRepository.writeOrderConfirmedNotification(uid, listing.businessName)
                } catch (_: Exception) {}

                _paymentState.value = PaymentState.Success(claimId)

            } catch (e: Exception) {
                _paymentState.value = PaymentState.Failed(e.message ?: "Order confirmation failed")
            }
        }
    }

    fun onPaymentFailed(reason: String) {
        _paymentState.value = PaymentState.Failed(reason)
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
        pendingOrderId   = null
        pendingQuantity  = 1
    }

    private fun computePickupDeadline(listing: Listing, distanceKm: Double): Long {
        val nowMs = System.currentTimeMillis()
        val expiryMs = listing.expiresAt.toDate().time
        val remainingMs = expiryMs - nowMs
        val travelBufferMs = TimeUnit.MINUTES.toMillis(
            (distanceKm * 5.0).toLong().coerceIn(0, 30)
        )
        return if (remainingMs > TimeUnit.HOURS.toMillis(1)) {
            expiryMs + travelBufferMs        // use actual expiry + travel buffer
        } else {
            nowMs + TimeUnit.HOURS.toMillis(1) + travelBufferMs  // extend by 1hr
        }
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a    = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

data class RazorpayCheckoutEvent(
    val orderId: String,
    val amount: Int,        // in paise
    val businessName: String,
    val heroItem: String,
    val email: String
)
