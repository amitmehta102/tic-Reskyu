package com.reskyu.consumer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import com.razorpay.PaymentResultListener
import com.reskyu.consumer.ui.navigation.ReskuNavGraph
import com.reskyu.consumer.ui.theme.ReskyuConsumerTheme

/**
 * MainActivity
 *
 * The single Activity in the app. Hosts the Compose NavHost.
 *
 * Implements [PaymentResultListener] so the Razorpay Android SDK can call back
 * after the native checkout sheet is dismissed. The result is forwarded to
 * [RazorpayPaymentBus] — a process-singleton StateFlow — which [ClaimScreen]
 * subscribes to in order to call the correct ViewModel method.
 */
class MainActivity : ComponentActivity(), PaymentResultListener {

    // Android 13+ requires runtime permission for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — FCM still works for background, foreground needs it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Handle listingId from notification tap (cold start)
        handleDeepLinkIntent(intent)

        setContent {
            ReskyuConsumerTheme {
                val navController = rememberNavController()
                ReskuNavGraph(navController = navController)
            }
        }
    }

    /**
     * Called when app is already open and user taps a notification.
     * FLAG_ACTIVITY_SINGLE_TOP routes here instead of a new onCreate.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val listingId = intent?.getStringExtra(NotificationDeepLinkBus.EXTRA_LISTING_ID)
        if (!listingId.isNullOrBlank()) {
            NotificationDeepLinkBus.postListingId(listingId)
        }
    }

    /** Called by Razorpay SDK on successful payment. */
    override fun onPaymentSuccess(paymentId: String?) {
        RazorpayPaymentBus.emit(
            RazorpayResult.Success(
                paymentId = paymentId ?: "",
                signature = ""   // SDK puts signature in onPaymentSuccess(String, PaymentData) override below
            )
        )
    }

    /**
     * Called by Razorpay SDK on successful payment with full payment data.
     * This overload provides the HMAC signature needed for server-side verification.
     */
    fun onPaymentSuccess(paymentId: String?, paymentData: com.razorpay.PaymentData?) {
        RazorpayPaymentBus.emit(
            RazorpayResult.Success(
                paymentId = paymentId ?: "",
                signature = paymentData?.signature ?: ""
            )
        )
    }

    /** Called by Razorpay SDK on payment failure. */
    override fun onPaymentError(code: Int, response: String?) {
        val reason = when (code) {
            0    -> "Network error. Please check your connection and try again."
            1    -> "Payment cancelled by user."
            2    -> "Payment failed. Please try another method."
            else -> response ?: "Payment could not be completed."
        }
        RazorpayPaymentBus.emit(RazorpayResult.Failure(reason))
    }
}