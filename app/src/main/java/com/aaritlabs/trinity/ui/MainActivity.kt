package com.aaritlabs.trinity.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.audio.SoundManager
import com.aaritlabs.trinity.data.GamePreferences
import com.aaritlabs.trinity.databinding.ActivityMainBinding
import com.aaritlabs.trinity.databinding.DialogHowToPlayBinding
import com.aaritlabs.trinity.databinding.DialogLevelsBinding
import com.aaritlabs.trinity.databinding.DialogSettingsBinding
import com.aaritlabs.trinity.databinding.DialogSkinBinding
import com.aaritlabs.trinity.databinding.DialogSoundQuickBinding
import com.aaritlabs.trinity.databinding.DialogThemeBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: GamePreferences
    private lateinit var soundManager: SoundManager

    private var heroHoverAnim: ObjectAnimator? = null
    private var playPulseAnim: ObjectAnimator? = null
    private val bonusWobbleHandler = Handler(Looper.getMainLooper())
    private var bonusWobbleRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = GamePreferences(this)
        soundManager = SoundManager.getInstance(this)

        setupSafeAreaInsets()
        setupUI()
        setupListeners()
        startHeroHoverAnimation()
        startPlayButtonPulse()
        startBonusWobbleSchedule()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        soundManager.resumeBgm()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        heroHoverAnim?.cancel()
        playPulseAnim?.cancel()
        bonusWobbleRunnable?.let { bonusWobbleHandler.removeCallbacks(it) }
    }

    private fun setupSafeAreaInsets() {
        val baseTopPadding = binding.topBar.paddingTop
        val baseBottomMargin = (binding.llActionButtons.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            val cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            val topInset = maxOf(systemBars.top, cutoutTop)

            binding.topBar.updatePadding(
                left = systemBars.left + 14,
                top = topInset + 10,
                right = systemBars.right + 14
            )

            binding.llActionButtons.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = baseBottomMargin + systemBars.bottom
            }

            insets
        }
    }

    private fun setupUI() {
        updateDashboard()
    }

    private fun updateDashboard() {
        val level = maxOf(prefs.highestUnlockedLevel, prefs.currentLevel)
        binding.tvCoins.text = prefs.coins.toString()
        binding.tvPlayButton.text = "CONTINUE $level"
    }

    private fun setupListeners() {
        binding.btnPlay.setOnClickListener {
            val levelToPlay = maxOf(prefs.highestUnlockedLevel, prefs.currentLevel)
            soundManager.playButton()
            soundManager.vibrate(30)
            it.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(90)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .setInterpolator(OvershootInterpolator())
                        .withEndAction {
                            launchGame(levelToPlay)
                        }.start()
                }.start()
        }

        binding.btnSkin.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showSkinModal()
        }

        binding.btnTheme.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showThemeModal()
        }

        binding.btnSound.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showSoundQuickModal()
        }

        binding.btnSettings.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showSettingsDialog()
        }

        binding.btnLevels.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showLevelsDialog()
        }

        binding.btnHowToPlay.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showHowToPlayDialog()
        }

        binding.btnDailyBonus.setOnClickListener {
            soundManager.playMatch()
            soundManager.vibrate(40)
            prefs.coins += 50
            updateDashboard()

            binding.btnDailyBonus.animate()
                .scaleX(1.12f).scaleY(1.12f)
                .setDuration(120)
                .withEndAction {
                    binding.btnDailyBonus.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                }.start()

            Toast.makeText(this, "+50 Coins Claimed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("LEVEL_NUM", level)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out)
    }

    private fun startHeroHoverAnimation() {
        heroHoverAnim = ObjectAnimator.ofFloat(binding.cardHeroBanner, "translationY", -8f, 8f).apply {
            duration = 2200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startPlayButtonPulse() {
        val pvhX = PropertyValuesHolder.ofFloat("scaleX", 0.985f, 1.018f)
        val pvhY = PropertyValuesHolder.ofFloat("scaleY", 0.985f, 1.018f)
        playPulseAnim = ObjectAnimator.ofPropertyValuesHolder(binding.btnPlay, pvhX, pvhY).apply {
            duration = 1300
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startBonusWobbleSchedule() {
        bonusWobbleRunnable = object : Runnable {
            override fun run() {
                val wobble = ObjectAnimator.ofFloat(binding.btnDailyBonus, "rotation", 0f, -6f, 6f, -4f, 4f, 0f).apply {
                    duration = 550
                }
                wobble.start()
                bonusWobbleHandler.postDelayed(this, 4500)
            }
        }
        bonusWobbleHandler.postDelayed(bonusWobbleRunnable!!, 3000)
    }

    private fun showSoundQuickModal() {
        val dialogBinding = DialogSoundQuickBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.switchSoundModal.isChecked = prefs.soundEnabled
        dialogBinding.switchMusicModal.isChecked = prefs.musicEnabled
        dialogBinding.switchVibrationModal.isChecked = prefs.vibrationEnabled

        dialogBinding.switchSoundModal.setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
            soundManager.playButton()
        }

        dialogBinding.switchMusicModal.setOnCheckedChangeListener { _, isChecked ->
            soundManager.setMusicEnabled(isChecked)
            soundManager.playButton()
        }

        dialogBinding.switchVibrationModal.setOnCheckedChangeListener { _, isChecked ->
            prefs.vibrationEnabled = isChecked
            if (isChecked) soundManager.vibrate(50)
        }

        dialogBinding.btnCloseSoundModal.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showThemeModal() {
        val dialogBinding = DialogThemeBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        fun updateThemeSelection() {
            val isLight = prefs.themeMode == GamePreferences.THEME_LIGHT
            val isDark = prefs.themeMode == GamePreferences.THEME_DARK
            val isSystem = !isLight && !isDark

            dialogBinding.rbThemeLight.isChecked = isLight
            dialogBinding.rbThemeDark.isChecked = isDark
            dialogBinding.rbThemeSystem.isChecked = isSystem

            dialogBinding.cardThemeLight.setBackgroundResource(
                if (isLight) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card
            )
            dialogBinding.cardThemeDark.setBackgroundResource(
                if (isDark) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card
            )
            dialogBinding.cardThemeSystem.setBackgroundResource(
                if (isSystem) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card
            )
        }

        updateThemeSelection()

        dialogBinding.cardThemeLight.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            prefs.themeMode = GamePreferences.THEME_LIGHT
            updateThemeSelection()
            dialog.dismiss()
        }

        dialogBinding.cardThemeDark.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            prefs.themeMode = GamePreferences.THEME_DARK
            updateThemeSelection()
            dialog.dismiss()
        }

        dialogBinding.cardThemeSystem.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            prefs.themeMode = GamePreferences.THEME_SYSTEM
            updateThemeSelection()
            dialog.dismiss()
        }

        dialogBinding.btnCloseThemeModal.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSkinModal() {
        val dialogBinding = DialogSkinBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        fun updateSkinBadges() {
            val currentSkin = prefs.equippedItemSkin
            val isFlowers = currentSkin == "skin_flowers"
            val isFruits = currentSkin == "skin_fruits"
            val isEmotions = currentSkin == "skin_emotions"
            val isAnimals = currentSkin == "skin_animals"
            val isSweets = currentSkin == "skin_sweets"

            dialogBinding.badgeSkinFlowers.visibility = if (isFlowers) android.view.View.VISIBLE else android.view.View.GONE
            dialogBinding.badgeSkinFruits.visibility = if (isFruits) android.view.View.VISIBLE else android.view.View.GONE
            dialogBinding.badgeSkinEmotions.visibility = if (isEmotions) android.view.View.VISIBLE else android.view.View.GONE
            dialogBinding.badgeSkinAnimals.visibility = if (isAnimals) android.view.View.VISIBLE else android.view.View.GONE
            dialogBinding.badgeSkinSweets.visibility = if (isSweets) android.view.View.VISIBLE else android.view.View.GONE

            dialogBinding.cardSkinFlowers.setBackgroundResource(if (isFlowers) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)
            dialogBinding.cardSkinFruits.setBackgroundResource(if (isFruits) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)
            dialogBinding.cardSkinEmotions.setBackgroundResource(if (isEmotions) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)
            dialogBinding.cardSkinAnimals.setBackgroundResource(if (isAnimals) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)
            dialogBinding.cardSkinSweets.setBackgroundResource(if (isSweets) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)
        }

        updateSkinBadges()

        fun equipSkin(skinId: String, skinName: String) {
            soundManager.playMatch()
            soundManager.vibrate(35)
            prefs.equippedItemSkin = skinId
            updateSkinBadges()
            Toast.makeText(this, "Equipped $skinName Skin!", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.cardSkinFlowers.setOnClickListener {
            equipSkin("skin_flowers", "Nature & Flowers")
        }

        dialogBinding.cardSkinFruits.setOnClickListener {
            equipSkin("skin_fruits", "Fruits & Food")
        }

        dialogBinding.cardSkinEmotions.setOnClickListener {
            equipSkin("skin_emotions", "Smileys & Emotions")
        }

        dialogBinding.cardSkinAnimals.setOnClickListener {
            equipSkin("skin_animals", "Cute Animals")
        }

        dialogBinding.cardSkinSweets.setOnClickListener {
            equipSkin("skin_sweets", "Sweet Bakery")
        }

        dialogBinding.btnCloseSkinModal.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showLevelsDialog() {
        val dialogBinding = DialogLevelsBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.tvLevelsProgress.text = "Unlocked: ${prefs.highestUnlockedLevel} / 1000 Levels"

        val adapter = LevelAdapter(this, totalLevels = 1000) { selectedLevel ->
            dialog.dismiss()
            soundManager.playButton()
            launchGame(selectedLevel)
        }

        dialogBinding.rvLevels.setHasFixedSize(true)
        dialogBinding.rvLevels.layoutManager = GridLayoutManager(this, 4)
        dialogBinding.rvLevels.adapter = adapter

        val targetPos = (prefs.highestUnlockedLevel - 1).coerceIn(0, 999)
        dialogBinding.rvLevels.post {
            dialogBinding.rvLevels.scrollToPosition(targetPos)
        }

        dialogBinding.btnCloseLevels.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.92f).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.switchSound.isChecked = prefs.soundEnabled
        dialogBinding.switchMusic.isChecked = prefs.musicEnabled
        dialogBinding.switchVibration.isChecked = prefs.vibrationEnabled

        fun updateSettingsLabels() {
            dialogBinding.tvCurrentThemeValue.text = when (prefs.themeMode) {
                GamePreferences.THEME_LIGHT -> "Light ☀️"
                GamePreferences.THEME_DARK -> "Dark 🌙"
                else -> "System ⚙️"
            }
            dialogBinding.tvCurrentSkinValue.text = when (prefs.equippedItemSkin) {
                "skin_flowers" -> "Flowers 🌺"
                "skin_fruits" -> "Fruits 🍓"
                "skin_emotions" -> "Smileys 🤩"
                "skin_animals" -> "Animals 🐼"
                "skin_sweets" -> "Bakery 🍦"
                else -> "Flowers 🌺"
            }
        }
        updateSettingsLabels()

        dialogBinding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
            soundManager.playButton()
        }

        dialogBinding.switchMusic.setOnCheckedChangeListener { _, isChecked ->
            soundManager.setMusicEnabled(isChecked)
            soundManager.playButton()
        }

        dialogBinding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.vibrationEnabled = isChecked
            if (isChecked) soundManager.vibrate(50)
        }

        dialogBinding.rowThemeSettings.setOnClickListener {
            soundManager.playButton()
            showThemeModal()
            updateSettingsLabels()
        }

        dialogBinding.rowSkinSettings.setOnClickListener {
            soundManager.playButton()
            showSkinModal()
            updateSettingsLabels()
        }

        dialogBinding.btnCloseSettings.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showHowToPlayDialog() {
        val dialogBinding = DialogHowToPlayBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.btnGotIt.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialogBinding.btnCloseHowToPlay.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
    }
}
