package com.reskyu.merchant

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reskyu.merchant.service.ListingExpiryWorker
import java.util.concurrent.TimeUnit

/**
 * Application class registered in AndroidManifest via android:name=".MerchantApplication".
 * Firebase is auto-initialized by the google-services plugin — no manual call needed.
 *
 * Schedules [ListingExpiryWorker] as a 15-minute repeating background job so that
 * listings past their expiry time are automatically removed from the live feed,
 * even when the merchant has the app closed.
 */
class MerchantApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── Schedule background listing expiry (15 min repeat) ────────────────
        val expiryRequest = PeriodicWorkRequestBuilder<ListingExpiryWorker>(
            repeatInterval     = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ListingExpiryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,   // don't reset timer if already scheduled
            expiryRequest
        )
    }
}

