package com.reskyu.consumer.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.reskyu.consumer.data.model.AppNotification
import com.reskyu.consumer.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * NotificationRepository
 *
 * Manages in-app notifications stored in Firestore at:
 *  /users/{uid}/notifications/{notifId}
 *
 * Notifications are written by the app itself (no Cloud Functions required):
 *  - On claim success → ORDER_CONFIRMED notification
 *  - On nearby drop → NEW_DROP notification (future)
 *
 * In dev mode (uid unavailable) falls back to in-memory sample data.
 */
class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()

    fun notifCollection(uid: String) =
        db.collection("users").document(uid).collection("notifications")

    // ── Real Firestore Listener ───────────────────────────────────────────────

    /**
     * Real-time Flow of the user's notification documents, newest first.
     * Falls back to empty list on error.
     */
    fun observeNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val reg = notifCollection(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())   // error — show empty state, not fake data
                    return@addSnapshotListener
                }
                val notifications = snapshot.documents.mapNotNull { doc ->
                    try {
                        val ts = doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: System.currentTimeMillis()
                        AppNotification(
                            id        = doc.id,
                            title     = doc.getString("title") ?: "",
                            body      = doc.getString("body") ?: "",
                            timestamp = ts,
                            isRead    = doc.getBoolean("isRead") ?: false,
                            type      = NotificationType.valueOf(
                                doc.getString("type") ?: "SYSTEM"
                            ),
                            // Populate deepLink so AlertRow can navigate to the listing
                            deepLink  = doc.getString("listingId")
                        )
                    } catch (_: Exception) { null }
                }
                // Return actual list (may be empty) — do NOT fall back to dev samples
                // for authenticated users; they should see an empty state
                trySend(notifications)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Writes an "Order Confirmed" notification to Firestore.
     * Called by ClaimViewModel after a successful claim transaction.
     */
    suspend fun writeOrderConfirmedNotification(uid: String, businessName: String) {
        val doc = mapOf(
            "title"     to "✅ Order Confirmed!",
            "body"      to "Your Reskyu drop from $businessName is ready. Show the pickup code at the counter.",
            "type"      to "ORDER",
            "timestamp" to Timestamp.now(),
            "isRead"    to false
        )
        notifCollection(uid).add(doc).await()
    }

    /**
     * Marks a single notification as read.
     */
    suspend fun markAsRead(uid: String, notifId: String) {
        notifCollection(uid).document(notifId).update("isRead", true).await()
    }

    /**
     * Deletes a notification document (swipe to dismiss).
     */
    suspend fun dismiss(uid: String, notifId: String) {
        notifCollection(uid).document(notifId).delete().await()
    }

    // ── Dev fallback ─────────────────────────────────────────────────────────

    fun devSampleNotifications(): List<AppNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            AppNotification(
                id = "notif_1", title = "🍱 New drop near you!",
                body = "The Bread Basket just posted Assorted Pastry Box for ₹99. Only 3 left!",
                timestamp = now - TimeUnit.MINUTES.toMillis(8),
                isRead = false, type = NotificationType.NEW_DROP
            ),
            AppNotification(
                id = "notif_2", title = "⚡ Last chance — 1 left!",
                body = "Green Leaf Café's Veg Thali expires in 45 min. Grab it for ₹79.",
                timestamp = now - TimeUnit.HOURS.toMillis(1),
                isRead = false, type = NotificationType.ALERT
            ),
            AppNotification(
                id = "notif_3", title = "✅ Order Confirmed",
                body = "Your claim at Spice Garden is confirmed. Show pickup code DEV003 at the counter.",
                timestamp = now - TimeUnit.HOURS.toMillis(5),
                isRead = true, type = NotificationType.ORDER
            ),
            AppNotification(
                id = "notif_4", title = "🌍 Your impact this week",
                body = "You've rescued 3 meals and saved 7.5 kg of CO₂ this week. Keep it up!",
                timestamp = now - TimeUnit.DAYS.toMillis(1),
                isRead = true, type = NotificationType.IMPACT
            )
        )
    }
}
