package com.aaritlabs.trinity.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("trinity_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var highestUnlockedLevel: Int
        get() = prefs.getInt("highest_level", 1)
        set(value) = prefs.edit().putInt("highest_level", value).apply()

    var coins: Int
        get() {
            val savedCoins = prefs.getInt("coins", 0)
            if (savedCoins == 500 && highestUnlockedLevel == 1 && getStarsForLevel(1) == 0) {
                coins = 0
                return 0
            }
            return savedCoins
        }
        set(value) = prefs.edit().putInt("coins", value).apply()

    var boosterUndo: Int
        get() = prefs.getInt("booster_undo", 3)
        set(value) = prefs.edit().putInt("booster_undo", value).apply()

    var boosterShuffle: Int
        get() = prefs.getInt("booster_shuffle", 3)
        set(value) = prefs.edit().putInt("booster_shuffle", value).apply()

    var boosterWand: Int
        get() = prefs.getInt("booster_wand", 3)
        set(value) = prefs.edit().putInt("booster_wand", value).apply()

    // --- Daily Gift (Once every 24 hours) ---
    var lastDailyGiftClaimTime: Long
        get() = prefs.getLong("last_daily_gift_claim_time", 0L)
        set(value) = prefs.edit().putLong("last_daily_gift_claim_time", value).apply()

    fun canClaimDailyGift(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - lastDailyGiftClaimTime
        return elapsed >= 24 * 60 * 60 * 1000L || lastDailyGiftClaimTime == 0L
    }

    fun getDailyGiftRemainingTimeMillis(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastDailyGiftClaimTime
        val cooldown = 24 * 60 * 60 * 1000L
        return if (elapsed < cooldown) cooldown - elapsed else 0L
    }

    fun claimDailyGift(): Boolean {
        if (canClaimDailyGift()) {
            coins += 50
            lastDailyGiftClaimTime = System.currentTimeMillis()
            return true
        }
        return false
    }

    // --- Daily Rewarded Video Ad Limits (5 times per day per booster) ---
    private fun checkAdDayReset() {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val savedDay = prefs.getString("last_ad_reset_day", "") ?: ""
        if (today != savedDay) {
            prefs.edit()
                .putString("last_ad_reset_day", today)
                .putInt("ads_remaining_undo", 5)
                .putInt("ads_remaining_shuffle", 5)
                .putInt("ads_remaining_wand", 5)
                .apply()
        }
    }

    var adsRemainingUndo: Int
        get() {
            checkAdDayReset()
            return prefs.getInt("ads_remaining_undo", 5)
        }
        set(value) = prefs.edit().putInt("ads_remaining_undo", value).apply()

    var adsRemainingShuffle: Int
        get() {
            checkAdDayReset()
            return prefs.getInt("ads_remaining_shuffle", 5)
        }
        set(value) = prefs.edit().putInt("ads_remaining_shuffle", value).apply()

    var adsRemainingWand: Int
        get() {
            checkAdDayReset()
            return prefs.getInt("ads_remaining_wand", 5)
        }
        set(value) = prefs.edit().putInt("ads_remaining_wand", value).apply()

    var isAdRemoved: Boolean
        get() = prefs.getBoolean("is_ad_removed", false)
        set(value) = prefs.edit().putBoolean("is_ad_removed", value).apply()

    var equippedItemSkin: String
        get() = prefs.getString("equipped_item_skin", "skin_flowers") ?: "skin_flowers"
        set(value) = prefs.edit().putString("equipped_item_skin", value).apply()

    fun isSkinUnlocked(skinId: String): Boolean {
        // Nature & Flowers theme is unlocked for default as requested
        if (skinId == "skin_flowers") return true
        if (prefs.getBoolean("all_skins_unlocked", false)) return true
        return prefs.getBoolean("skin_unlocked_$skinId", false)
    }

    fun unlockSkin(skinId: String) {
        prefs.edit().putBoolean("skin_unlocked_$skinId", true).apply()
    }

    fun unlockAllSkins() {
        prefs.edit()
            .putBoolean("all_skins_unlocked", true)
            .putBoolean("skin_unlocked_skin_fruits", true)
            .putBoolean("skin_unlocked_skin_emotions", true)
            .putBoolean("skin_unlocked_skin_animals", true)
            .putBoolean("skin_unlocked_skin_sweets", true)
            .apply()
    }

    fun areAllSkinsUnlocked(): Boolean {
        return prefs.getBoolean("all_skins_unlocked", false) ||
                (isSkinUnlocked("skin_fruits") &&
                 isSkinUnlocked("skin_emotions") &&
                 isSkinUnlocked("skin_animals") &&
                 isSkinUnlocked("skin_sweets"))
    }

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) = prefs.edit().putBoolean("music_enabled", value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", THEME_SYSTEM)
        set(value) {
            prefs.edit().putInt("theme_mode", value).apply()
            applyTheme(value)
        }

    fun applySavedTheme() {
        applyTheme(themeMode)
    }

    private fun applyTheme(mode: Int) {
        val nightMode = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getStarsForLevel(level: Int): Int {
        return prefs.getInt("stars_level_$level", 0)
    }

    fun setStarsForLevel(level: Int, stars: Int) {
        val current = getStarsForLevel(level)
        if (stars > current) {
            prefs.edit().putInt("stars_level_$level", stars).apply()
        }
    }

    var completedLevelsCount: Int
        get() = prefs.getInt("completed_levels_count", 0)
        set(value) = prefs.edit().putInt("completed_levels_count", value).apply()

    fun unlockNextLevel(completedLevel: Int) {
        val next = (completedLevel + 1).coerceAtMost(1000)
        if (completedLevel >= highestUnlockedLevel) {
            highestUnlockedLevel = next
        }
        currentLevel = next
    }
}
