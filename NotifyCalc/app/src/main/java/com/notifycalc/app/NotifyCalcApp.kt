package com.notifycalc.app

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * Application entry point.
 *
 * Opts every activity into Material You dynamic colors on Android 12+.
 * On older devices the static Material 3 palette from the theme is used.
 */
class NotifyCalcApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
