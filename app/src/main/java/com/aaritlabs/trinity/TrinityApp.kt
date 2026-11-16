package com.aaritlabs.trinity

import android.app.Application
import com.aaritlabs.trinity.data.GamePreferences

class TrinityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply user-selected theme before any activity or splash screen inflates
        GamePreferences(this).applySavedTheme()
    }
}
