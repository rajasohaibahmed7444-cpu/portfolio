package com.notifycalc.app.data.repository

import android.util.Log
import com.notifycalc.app.data.model.NotificationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single entry point for everything that happens to a captured notification.
 *
 * The repository deliberately has no storage: notifications are held only as
 * the in-memory [lastProcessed] value used by the Settings screen. When cloud
 * synchronization is added later, [uploadNotification] is the only place that
 * needs to change — swap the log statement for a call to the remote data
 * source (REST client, gRPC stub, work enqueue, ...).
 */
object NotificationRepository {

    private const val TAG = "NotificationRepository"

    private val _lastProcessed = MutableStateFlow<NotificationData?>(null)

    /** The most recently processed notification, or null if none yet. */
    val lastProcessed: StateFlow<NotificationData?> = _lastProcessed.asStateFlow()

    /**
     * Uploads a notification to the (future) backup backend.
     *
     * This is a placeholder implementation: it performs no network I/O and
     * stores nothing on disk. It only records the notification as the last
     * processed one so the UI can surface backup activity.
     */
    suspend fun uploadNotification(notification: NotificationData) {
        withContext(Dispatchers.IO) {
            // Cloud synchronization integration point. Intentionally a no-op.
            Log.d(
                TAG,
                "uploadNotification: id=${notification.notificationId} " +
                    "package=${notification.packageName} posted=${notification.timestamp}"
            )
        }
        _lastProcessed.value = notification
    }
}
