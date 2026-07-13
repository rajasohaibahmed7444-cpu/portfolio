package com.notifycalc.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.notifycalc.app.R
import com.notifycalc.app.data.preferences.AppPreferences
import com.notifycalc.app.databinding.ActivityWelcomeBinding
import com.notifycalc.app.ui.main.MainActivity
import com.notifycalc.app.util.NotificationAccessUtil

/**
 * One-time welcome screen shown on first launch.
 *
 * Explains that the calculator works fully on its own and that Notification
 * Backup is an optional feature requiring the system Notification access
 * permission. Whatever the user picks, the screen never appears again.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)

        binding.btnEnableBackup.setOnClickListener { completeWelcome(enableBackup = true) }
        binding.btnSkipBackup.setOnClickListener { completeWelcome(enableBackup = false) }
    }

    private fun completeWelcome(enableBackup: Boolean) {
        preferences.isBackupEnabled = enableBackup
        preferences.isWelcomeCompleted = true

        // Put the calculator underneath so the user lands there when they
        // come back from system settings (or immediately when skipping).
        startActivity(Intent(this, MainActivity::class.java))

        if (enableBackup) {
            val opened = NotificationAccessUtil.openNotificationAccessSettings(this)
            if (!opened) {
                Toast.makeText(this, R.string.error_open_settings, Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }
}
