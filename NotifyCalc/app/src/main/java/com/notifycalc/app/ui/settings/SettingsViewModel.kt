package com.notifycalc.app.ui.settings

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifycalc.app.data.model.NotificationData
import com.notifycalc.app.data.preferences.AppPreferences
import com.notifycalc.app.data.repository.NotificationRepository
import com.notifycalc.app.util.NotificationAccessUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable UI state for the settings screen. */
data class SettingsUiState(
    /** The user's in-app opt-in to notification backup. */
    val isBackupEnabled: Boolean = false,
    /** Whether the system Notification access permission is granted. */
    val hasNotificationAccess: Boolean = false,
    /** The most recently processed notification, if any, this process. */
    val lastProcessed: NotificationData? = null,
    /** Human readable application version. */
    val appVersion: String = ""
)

/**
 * ViewModel for the settings screen. Combines the stored backup preference,
 * the live system permission state and the repository's last processed
 * notification into a single [SettingsUiState].
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Keep "last processed notification" live while the screen is open.
        viewModelScope.launch {
            NotificationRepository.lastProcessed.collect { refresh() }
        }
    }

    /** Re-reads preference and permission state; call from onResume. */
    fun refresh() {
        _uiState.value = buildState()
    }

    /** Persists the user's backup opt-in choice. */
    fun setBackupEnabled(enabled: Boolean) {
        preferences.isBackupEnabled = enabled
        refresh()
    }

    private fun buildState(): SettingsUiState {
        val context: Context = getApplication<Application>()
        return SettingsUiState(
            isBackupEnabled = preferences.isBackupEnabled,
            hasNotificationAccess = NotificationAccessUtil.isNotificationAccessGranted(context),
            lastProcessed = NotificationRepository.lastProcessed.value,
            appVersion = readAppVersion(context)
        )
    }

    private fun readAppVersion(context: Context): String = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: DEFAULT_VERSION
    } catch (_: PackageManager.NameNotFoundException) {
        DEFAULT_VERSION
    }

    private companion object {
        const val DEFAULT_VERSION = "1.0"
    }
}
