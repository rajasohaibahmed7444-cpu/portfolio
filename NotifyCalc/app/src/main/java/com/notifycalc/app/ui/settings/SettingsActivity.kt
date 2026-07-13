package com.notifycalc.app.ui.settings

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.notifycalc.app.R
import com.notifycalc.app.databinding.ActivitySettingsBinding
import com.notifycalc.app.util.NotificationAccessUtil
import kotlinx.coroutines.launch

/**
 * Settings screen: notification backup status and controls, the last
 * processed notification, app version and an about dialog.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    private val backupSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            viewModel.setBackupEnabled(isChecked)
            if (isChecked && !NotificationAccessUtil.isNotificationAccessGranted(this)) {
                promptForNotificationAccess()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnOpenAccess.setOnClickListener { openNotificationAccessSettings() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have toggled Notification access in system settings.
        viewModel.refresh()
    }

    private fun render(state: SettingsUiState) {
        // Detach the listener while syncing the switch programmatically.
        binding.switchBackup.setOnCheckedChangeListener(null)
        binding.switchBackup.isChecked = state.isBackupEnabled
        binding.switchBackup.setOnCheckedChangeListener(backupSwitchListener)

        binding.textBackupStatus.text = getString(
            when {
                state.isBackupEnabled && state.hasNotificationAccess ->
                    R.string.backup_status_active

                state.isBackupEnabled -> R.string.backup_status_awaiting_permission
                else -> R.string.backup_status_disabled
            }
        )
        binding.textPermissionWarning.visibility =
            if (state.isBackupEnabled && !state.hasNotificationAccess) View.VISIBLE else View.GONE

        val last = state.lastProcessed
        if (last == null) {
            binding.textLastNotification.text = getString(R.string.last_notification_empty)
            binding.textLastNotificationTime.visibility = View.GONE
        } else {
            val title = last.title.ifEmpty { getString(R.string.last_notification_no_title) }
            binding.textLastNotification.text =
                getString(R.string.last_notification_format, last.appName, title)
            binding.textLastNotificationTime.visibility = View.VISIBLE
            binding.textLastNotificationTime.text = DateUtils.getRelativeTimeSpanString(
                last.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        }

        binding.textAppVersion.text = state.appVersion
    }

    private fun promptForNotificationAccess() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_access_title)
            .setMessage(R.string.dialog_access_message)
            .setPositiveButton(R.string.dialog_open_settings) { _, _ ->
                openNotificationAccessSettings()
            }
            .setNegativeButton(R.string.dialog_later, null)
            .show()
    }

    private fun openNotificationAccessSettings() {
        val opened = NotificationAccessUtil.openNotificationAccessSettings(this)
        if (!opened) {
            Snackbar.make(binding.root, R.string.error_open_settings, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_dialog_title)
            .setMessage(R.string.about_dialog_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
