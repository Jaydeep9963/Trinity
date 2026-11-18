package com.aaritlabs.trinity

import android.app.Application
import com.aaritlabs.trinity.ads.AdMobManager
import com.aaritlabs.trinity.billing.BillingManager
import com.aaritlabs.trinity.data.GamePreferences

class TrinityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply user-selected theme before any activity or splash screen inflates
        GamePreferences(this).applySavedTheme()

        // Initialize Google AdMob & Google Play Billing
        AdMobManager.getInstance(this).initialize()
        BillingManager.getInstance(this).startConnection()
    }
}
