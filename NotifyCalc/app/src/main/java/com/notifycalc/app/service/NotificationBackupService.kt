package com.notifycalc.app.service

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.notifycalc.app.data.model.NotificationData
import com.notifycalc.app.data.preferences.AppPreferences
import com.notifycalc.app.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Official [NotificationListenerService] implementation for the optional
 * Notification Backup feature.
 *
 * The system binds this service once the user grants Notification access in
 * system settings. Every newly posted notification is converted into a
 * [NotificationData] snapshot and handed to [NotificationRepository]. If the
 * user has not opted in to backup inside the app, incoming notifications are
 * ignored. Nothing is ever persisted locally, and every step is guarded so a
 * misbehaving notification can never crash the app.
 */
class NotificationBackupService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        try {
            // The user can revoke the in-app opt-in independently of the
            // system permission; honor it before touching the notification.
            if (!preferences.isBackupEnabled) return
            // Never process this app's own notifications.
            if (statusBarNotification.packageName == packageName) return

            val data = statusBarNotification.toNotificationData()
            serviceScope.launch {
                try {
                    NotificationRepository.uploadNotification(data)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to hand notification to repository", t)
                }
            }
        } catch (t: Throwable) {
            // A malformed notification must never take the listener down.
            Log.e(TAG, "Failed to process posted notification", t)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private fun StatusBarNotification.toNotificationData(): NotificationData {
        val notification = notification
        val extras = notification.extras

        val messagingStyle = try {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not extract messaging style", t)
            null
        }
        val sender = messagingStyle?.messages?.lastOrNull()?.person?.name?.toString().orEmpty()
        val conversationTitle = messagingStyle?.conversationTitle?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString().orEmpty()

        return NotificationData(
            appName = resolveAppName(packageName),
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            timestamp = postTime,
            notificationId = id,
            notificationKey = key.orEmpty(),
            conversationTitle = conversationTitle,
            sender = sender,
            category = notification.category.orEmpty(),
            priority = resolveImportance(key),
            channelId = notification.channelId.orEmpty(),
            groupKey = groupKey.orEmpty(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    private fun resolveAppName(packageName: String): String = try {
        val pm = packageManager
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(applicationInfo).toString()
    } catch (_: Throwable) {
        packageName
    }

    /** Channel importance from the current ranking; the modern "priority". */
    private fun resolveImportance(key: String?): Int = try {
        val ranking = Ranking()
        if (key != null && currentRanking.getRanking(key, ranking)) {
            ranking.importance
        } else {
            NotificationManager.IMPORTANCE_UNSPECIFIED
        }
    } catch (_: Throwable) {
        NotificationManager.IMPORTANCE_UNSPECIFIED
    }

    private companion object {
        const val TAG = "NotificationBackup"
    }
}
