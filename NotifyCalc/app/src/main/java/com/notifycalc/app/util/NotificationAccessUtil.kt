package com.notifycalc.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Helpers around the system "Notification access" (notification listener)
 * permission, which can only be granted by the user in system settings.
 */
object NotificationAccessUtil {

    /** True when this app is an enabled notification listener. */
    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    /**
     * Opens the system Notification access settings screen.
     *
     * @return true if the settings screen could be launched, false when no
     *         activity on the device handles the intent.
     */
    fun openNotificationAccessSettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
