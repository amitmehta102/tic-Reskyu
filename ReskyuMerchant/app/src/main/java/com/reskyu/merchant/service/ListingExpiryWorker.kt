package com.reskyu.merchant.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.reskyu.merchant.data.repository.ListingRepository

/**
 * Background WorkManager worker that expires overdue listings once every 15 minutes,
 * even when the app is not in the foreground.
 *
 * Scheduled from [MerchantApplication] as a [androidx.work.PeriodicWorkRequest].
 *
 * Flow:
 *  1. Reads current user UID from FirebaseAuth (may return null if user is signed out)
 *  2. Calls [ListingRepository.expireOverdueListings] — batch-writes EXPIRED status
 *     to any OPEN/CLOSING listing whose expiresAt ≤ System.currentTimeMillis()
 *  3. Reports SUCCESS (never retries — idempotent, will run again in 15 min)
 */
class ListingExpiryWorker(
    context : Context,
    params  : WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success()   // Nobody signed in — nothing to expire

        return try {
            val expired = ListingRepository().expireOverdueListings(uid)
            android.util.Log.d("ListingExpiryWorker", "Expired $expired listing(s) for $uid")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("ListingExpiryWorker", "Failed to expire listings: ${e.message}")
            Result.success()   // Don't retry — next periodic run will catch it
        }
    }

    companion object {
        const val WORK_NAME = "listing_expiry_periodic"
    }
}
