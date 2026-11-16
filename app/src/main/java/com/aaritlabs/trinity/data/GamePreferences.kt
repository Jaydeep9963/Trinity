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
        get() = prefs.getInt("coins", 500)
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

    var equippedItemSkin: String
        get() = prefs.getString("equipped_item_skin", "skin_fruits") ?: "skin_fruits"
        set(value) = prefs.edit().putString("equipped_item_skin", value).apply()

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

    fun unlockNextLevel(completedLevel: Int) {
        if (completedLevel >= highestUnlockedLevel) {
            highestUnlockedLevel = completedLevel + 1
        }
        currentLevel = completedLevel + 1
    }
}
