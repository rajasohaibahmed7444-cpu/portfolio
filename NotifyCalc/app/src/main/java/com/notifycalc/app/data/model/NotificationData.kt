package com.notifycalc.app.data.model

/**
 * Immutable snapshot of a single notification delivered to the device.
 *
 * Instances are created by
 * [com.notifycalc.app.service.NotificationBackupService] and handed to
 * [com.notifycalc.app.data.repository.NotificationRepository]. They are kept
 * in memory only and are never persisted locally.
 */
data class NotificationData(
    /** Human readable name of the app that posted the notification. */
    val appName: String,
    /** Package name of the app that posted the notification. */
    val packageName: String,
    /** Notification title ([android.app.Notification.EXTRA_TITLE]). */
    val title: String,
    /** Notification text ([android.app.Notification.EXTRA_TEXT]). */
    val text: String,
    /** Expanded text ([android.app.Notification.EXTRA_BIG_TEXT]), if any. */
    val bigText: String,
    /** Wall-clock time the notification was posted, in epoch milliseconds. */
    val timestamp: Long,
    /** Id supplied by the posting app. */
    val notificationId: Int,
    /** Unique key assigned by the system ranker. */
    val notificationKey: String,
    /** Conversation title for messaging-style notifications, if any. */
    val conversationTitle: String,
    /** Sender of the most recent message for messaging-style notifications. */
    val sender: String,
    /** Notification category ([android.app.Notification.getCategory]), if any. */
    val category: String,
    /** Channel importance reported by the notification ranker. */
    val priority: Int,
    /** Id of the notification channel the notification was posted to. */
    val channelId: String,
    /** Group key used to bundle related notifications, if any. */
    val groupKey: String,
    /** Manufacturer and model of this device. */
    val deviceModel: String,
    /** Android version running on this device. */
    val androidVersion: String
)
