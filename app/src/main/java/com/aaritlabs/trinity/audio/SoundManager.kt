package com.aaritlabs.trinity.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.data.GamePreferences

class SoundManager private constructor(private val context: Context) {
    private val prefs = GamePreferences(context)
    private val soundPool: SoundPool
    private val vibrator: Vibrator?

    private val soundMap = mutableMapOf<Int, Int>()
    private var bgmPlayer: MediaPlayer? = null
    private var isBgmPausedByUser = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        loadSounds()
    }

    private fun loadSounds() {
        soundMap[SOUND_CLICK] = soundPool.load(context, R.raw.sfx_tile_click, 1)
        soundMap[SOUND_MATCH] = soundPool.load(context, R.raw.sfx_tile_match, 1)
        soundMap[SOUND_COMBO] = soundPool.load(context, R.raw.sfx_combo, 1)
        soundMap[SOUND_UNDO] = soundPool.load(context, R.raw.sfx_undo, 1)
        soundMap[SOUND_SHUFFLE] = soundPool.load(context, R.raw.sfx_shuffle, 1)
        soundMap[SOUND_WAND] = soundPool.load(context, R.raw.sfx_wand, 1)
        soundMap[SOUND_WIN] = soundPool.load(context, R.raw.sfx_level_win, 1)
        soundMap[SOUND_FAIL] = soundPool.load(context, R.raw.sfx_level_fail, 1)
        soundMap[SOUND_BUTTON] = soundPool.load(context, R.raw.sfx_button, 1)
        soundMap[SOUND_ALERT] = soundPool.load(context, R.raw.sfx_alert, 1)
    }

    fun playClick() = playSound(SOUND_CLICK)
    fun playMatch() = playSound(SOUND_MATCH)
    fun playCombo() = playSound(SOUND_COMBO)
    fun playUndo() = playSound(SOUND_UNDO)
    fun playShuffle() = playSound(SOUND_SHUFFLE)
    fun playWand() = playSound(SOUND_WAND)
    fun playWin() = playSound(SOUND_WIN)
    fun playFail() = playSound(SOUND_FAIL)
    fun playButton() = playSound(SOUND_BUTTON)
    fun playAlert() = playSound(SOUND_ALERT)

    private fun playSound(soundIdKey: Int) {
        if (!prefs.soundEnabled) return
        val soundId = soundMap[soundIdKey] ?: return
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    // --- Background Music Management ---

    fun startBgm() {
        if (!prefs.musicEnabled) return
        try {
            if (bgmPlayer == null) {
                bgmPlayer = MediaPlayer.create(context, R.raw.bgm_trinity)?.apply {
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                }
            }
            if (bgmPlayer?.isPlaying == false) {
                bgmPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing BGM", e)
        }
    }

    fun pauseBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error pausing BGM", e)
        }
    }

    fun resumeBgm() {
        if (prefs.musicEnabled) {
            startBgm()
        }
    }

    fun stopBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Error stopping BGM", e)
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        prefs.musicEnabled = enabled
        if (enabled) {
            startBgm()
        } else {
            pauseBgm()
        }
    }

    fun vibrate(durationMs: Long = 40) {
        if (!prefs.vibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopBgm()
        soundPool.release()
    }

    companion object {
        private const val SOUND_CLICK = 1
        private const val SOUND_MATCH = 2
        private const val SOUND_COMBO = 3
        private const val SOUND_UNDO = 4
        private const val SOUND_SHUFFLE = 5
        private const val SOUND_WAND = 6
        private const val SOUND_WIN = 7
        private const val SOUND_FAIL = 8
        private const val SOUND_BUTTON = 9
        private const val SOUND_ALERT = 10

        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
