package com.notifycalc.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.notifycalc.app.data.model.HistoryEntry
import androidx.core.content.edit

/**
 * Thin wrapper around [SharedPreferences] for the app's small amount of
 * persistent state: the one-time welcome flow flag, the user's notification
 * backup opt-in, and the calculator history.
 *
 * Notification content itself is intentionally never written here.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the user has made a choice on the welcome screen. */
    var isWelcomeCompleted: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(KEY_WELCOME_COMPLETED, value) }

    /** True when the user has opted in to the notification backup feature. */
    var isBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BACKUP_ENABLED, value) }

    /** Persists the calculation history, newest entry first. */
    fun saveHistory(entries: List<HistoryEntry>) {
        val serialized = entries.joinToString(ENTRY_SEPARATOR) { entry ->
            listOf(entry.expression, entry.result, entry.timestamp.toString())
                .joinToString(FIELD_SEPARATOR)
        }
        prefs.edit { putString(KEY_HISTORY, serialized) }
    }

    /** Loads the persisted calculation history; malformed entries are skipped. */
    fun loadHistory(): List<HistoryEntry> {
        val serialized = prefs.getString(KEY_HISTORY, null).orEmpty()
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(ENTRY_SEPARATOR).mapNotNull { raw ->
            val fields = raw.split(FIELD_SEPARATOR)
            if (fields.size != 3) return@mapNotNull null
            val timestamp = fields[2].toLongOrNull() ?: return@mapNotNull null
            HistoryEntry(expression = fields[0], result = fields[1], timestamp = timestamp)
        }
    }

    private companion object {
        const val PREFS_NAME = "notifycalc_prefs"
        const val KEY_WELCOME_COMPLETED = "welcome_completed"
        const val KEY_BACKUP_ENABLED = "backup_enabled"
        const val KEY_HISTORY = "calculation_history"

        // Control characters that cannot appear in calculator expressions.
        const val ENTRY_SEPARATOR = "\u001E"
        const val FIELD_SEPARATOR = "\u001F"
    }
}
