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

class MainActivity : ComponentActivity(), PaymentResultListener {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleDeepLinkIntent(intent)

        setContent {
            ReskyuConsumerTheme {
                val navController = rememberNavController()
                ReskuNavGraph(navController = navController)
            }
        }
    }

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

    override fun onPaymentSuccess(paymentId: String?) {
        RazorpayPaymentBus.emit(
            RazorpayResult.Success(
                paymentId = paymentId ?: "",
                signature = ""   // SDK puts signature in onPaymentSuccess(String, PaymentData) override below
            )
        )
    }

    fun onPaymentSuccess(paymentId: String?, paymentData: com.razorpay.PaymentData?) {
        RazorpayPaymentBus.emit(
            RazorpayResult.Success(
                paymentId = paymentId ?: "",
                signature = paymentData?.signature ?: ""
            )
        )
    }

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
