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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.ads.AdMobManager
import com.aaritlabs.trinity.audio.SoundManager
import com.aaritlabs.trinity.billing.BillingManager
import com.aaritlabs.trinity.data.GamePreferences
import com.aaritlabs.trinity.databinding.ActivityMainBinding
import com.aaritlabs.trinity.databinding.DialogHowToPlayBinding
import com.aaritlabs.trinity.databinding.DialogLevelsBinding
import com.aaritlabs.trinity.databinding.DialogSettingsBinding
import com.aaritlabs.trinity.databinding.DialogShopBinding
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
        if (prefs.isAdRemoved) {
            binding.bannerAdContainer.visibility = android.view.View.GONE
        } else {
            binding.adViewMain.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBgm()
        binding.adViewMain.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        heroHoverAnim?.cancel()
        playPulseAnim?.cancel()
        bonusWobbleRunnable?.let { bonusWobbleHandler.removeCallbacks(it) }
        binding.adViewMain.destroy()
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

            if (!prefs.isAdRemoved) {
                binding.bannerAdContainer.updatePadding(bottom = systemBars.bottom)
                binding.llActionButtons.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                    bottomMargin = 10
                }
            } else {
                binding.bannerAdContainer.visibility = android.view.View.GONE
                binding.llActionButtons.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                    bottomMargin = baseBottomMargin + systemBars.bottom
                }
            }

            insets
        }
    }

    private fun setupUI() {
        updateDashboard()
        setupBannerAd()
    }

    private fun setupBannerAd() {
        if (prefs.isAdRemoved) {
            binding.bannerAdContainer.visibility = android.view.View.GONE
        } else {
            binding.bannerAdContainer.visibility = android.view.View.VISIBLE
            AdMobManager.getInstance(this).loadBannerAd(binding.adViewMain, binding.bannerAdContainer)
        }
    }

    private fun updateDashboard() {
        val level = maxOf(prefs.highestUnlockedLevel, prefs.currentLevel)
        binding.tvCoins.text = prefs.coins.toString()
        binding.tvPlayButton.text = "CONTINUE $level"
        updateDailyGiftUI()
    }

    private fun updateDailyGiftUI() {
        if (prefs.canClaimDailyGift()) {
            binding.tvDailyGiftText.text = "Free Daily Gift: +50 Coins!"
            binding.tvDailyGiftText.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
            binding.btnDailyBonus.alpha = 1.0f
        } else {
            val remainingMs = prefs.getDailyGiftRemainingTimeMillis()
            val hours = remainingMs / (1000 * 60 * 60)
            val minutes = (remainingMs / (1000 * 60)) % 60
            binding.tvDailyGiftText.text = "Daily Gift: Claimed (Next in ${hours}h ${minutes}m)"
            binding.tvDailyGiftText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            binding.btnDailyBonus.alpha = 0.65f
        }
    }

    private fun setupListeners() {
        binding.llCoins.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showShopDialog()
        }

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
            if (prefs.canClaimDailyGift()) {
                prefs.claimDailyGift()
                soundManager.playMatch()
                soundManager.vibrate(40)
                updateDashboard()

                binding.btnDailyBonus.animate()
                    .scaleX(1.12f).scaleY(1.12f)
                    .setDuration(120)
                    .withEndAction {
                        binding.btnDailyBonus.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                    }.start()

                Toast.makeText(this, "+50 Coins Claimed!", Toast.LENGTH_SHORT).show()
            } else {
                soundManager.playAlert()
                soundManager.vibrate(25)
                val remainingMs = prefs.getDailyGiftRemainingTimeMillis()
                val hours = remainingMs / (1000 * 60 * 60)
                val minutes = (remainingMs / (1000 * 60)) % 60
                Toast.makeText(this, "Next daily gift available in ${hours}h ${minutes}m", Toast.LENGTH_SHORT).show()
            }
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
                if (prefs.canClaimDailyGift()) {
                    val wobble = ObjectAnimator.ofFloat(binding.btnDailyBonus, "rotation", 0f, -6f, 6f, -4f, 4f, 0f).apply {
                        duration = 550
                    }
                    wobble.start()
                }
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

            val unlockedFruits = prefs.isSkinUnlocked("skin_fruits")
            val unlockedEmotions = prefs.isSkinUnlocked("skin_emotions")
            val unlockedAnimals = prefs.isSkinUnlocked("skin_animals")
            val unlockedSweets = prefs.isSkinUnlocked("skin_sweets")

            // Flowers: always unlocked for default
            dialogBinding.badgeSkinFlowers.visibility = if (isFlowers) android.view.View.VISIBLE else android.view.View.GONE
            dialogBinding.cardSkinFlowers.setBackgroundResource(if (isFlowers) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)

            // Fruits ($2.99)
            if (unlockedFruits) {
                dialogBinding.priceSkinFruits.visibility = android.view.View.GONE
                dialogBinding.badgeSkinFruits.visibility = if (isFruits) android.view.View.VISIBLE else android.view.View.GONE
            } else {
                dialogBinding.badgeSkinFruits.visibility = android.view.View.GONE
                dialogBinding.priceSkinFruits.visibility = android.view.View.VISIBLE
            }
            dialogBinding.cardSkinFruits.setBackgroundResource(if (isFruits) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)

            // Emotions / Smileys ($2.99)
            if (unlockedEmotions) {
                dialogBinding.priceSkinEmotions.visibility = android.view.View.GONE
                dialogBinding.badgeSkinEmotions.visibility = if (isEmotions) android.view.View.VISIBLE else android.view.View.GONE
            } else {
                dialogBinding.badgeSkinEmotions.visibility = android.view.View.GONE
                dialogBinding.priceSkinEmotions.visibility = android.view.View.VISIBLE
            }
            dialogBinding.cardSkinEmotions.setBackgroundResource(if (isEmotions) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)

            // Animals ($2.99)
            if (unlockedAnimals) {
                dialogBinding.priceSkinAnimals.visibility = android.view.View.GONE
                dialogBinding.badgeSkinAnimals.visibility = if (isAnimals) android.view.View.VISIBLE else android.view.View.GONE
            } else {
                dialogBinding.badgeSkinAnimals.visibility = android.view.View.GONE
                dialogBinding.priceSkinAnimals.visibility = android.view.View.VISIBLE
            }
            dialogBinding.cardSkinAnimals.setBackgroundResource(if (isAnimals) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)

            // Sweets / Bakery ($2.99)
            if (unlockedSweets) {
                dialogBinding.priceSkinSweets.visibility = android.view.View.GONE
                dialogBinding.badgeSkinSweets.visibility = if (isSweets) android.view.View.VISIBLE else android.view.View.GONE
            } else {
                dialogBinding.badgeSkinSweets.visibility = android.view.View.GONE
                dialogBinding.priceSkinSweets.visibility = android.view.View.VISIBLE
            }
            dialogBinding.cardSkinSweets.setBackgroundResource(if (isSweets) R.drawable.bg_dialog_item_card_selected else R.drawable.bg_dialog_item_card)

            // All Skins Bundle ($9.99)
            if (prefs.areAllSkinsUnlocked()) {
                dialogBinding.tvAllSkinsBadge.text = "ALL UNLOCKED"
                dialogBinding.btnUnlockAllSkins.alpha = 0.6f
            } else {
                dialogBinding.tvAllSkinsBadge.text = "$9.99"
                dialogBinding.btnUnlockAllSkins.alpha = 1.0f
            }
        }

        updateSkinBadges()

        fun equipSkin(skinId: String, skinName: String) {
            soundManager.playMatch()
            soundManager.vibrate(35)
            prefs.equippedItemSkin = skinId
            updateSkinBadges()
            Toast.makeText(this, "Equipped $skinName Skin!", Toast.LENGTH_SHORT).show()
        }

        fun handleSkinClick(skinId: String, productId: String, skinName: String) {
            if (prefs.isSkinUnlocked(skinId)) {
                equipSkin(skinId, skinName)
            } else {
                soundManager.playButton()
                BillingManager.getInstance(this).launchPurchaseFlow(this, productId) { success, msg ->
                    if (success) {
                        equipSkin(skinId, skinName)
                        Toast.makeText(this, "$skinName Unlocked!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    updateSkinBadges()
                }
            }
        }

        dialogBinding.cardSkinFlowers.setOnClickListener {
            equipSkin("skin_flowers", "Nature & Flowers")
        }

        dialogBinding.cardSkinFruits.setOnClickListener {
            handleSkinClick("skin_fruits", BillingManager.PRODUCT_SKIN_FRUITS, "Fruits & Food")
        }

        dialogBinding.cardSkinEmotions.setOnClickListener {
            handleSkinClick("skin_emotions", BillingManager.PRODUCT_SKIN_SMILEYS, "Smileys & Emotions")
        }

        dialogBinding.cardSkinAnimals.setOnClickListener {
            handleSkinClick("skin_animals", BillingManager.PRODUCT_SKIN_ANIMALS, "Cute Animals")
        }

        dialogBinding.cardSkinSweets.setOnClickListener {
            handleSkinClick("skin_sweets", BillingManager.PRODUCT_SKIN_BAKERY, "Sweet Bakery")
        }

        dialogBinding.btnUnlockAllSkins.setOnClickListener {
            if (prefs.areAllSkinsUnlocked()) {
                soundManager.playButton()
                Toast.makeText(this, "All skins are already unlocked!", Toast.LENGTH_SHORT).show()
            } else {
                soundManager.playButton()
                BillingManager.getInstance(this).launchPurchaseFlow(this, BillingManager.PRODUCT_ALL_SKINS) { success, msg ->
                    if (success) {
                        Toast.makeText(this, "All Skins Unlocked Forever!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    updateSkinBadges()
                }
            }
        }

        dialogBinding.btnCloseSkinModal.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showShopDialog() {
        val dialogBinding = DialogShopBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        fun updateShopUI() {
            if (prefs.isAdRemoved) {
                dialogBinding.tvPriceRemoveAds.text = "REMOVED"
                dialogBinding.btnShopRemoveAds.alpha = 0.6f
            } else {
                dialogBinding.tvPriceRemoveAds.text = "$4.99"
                dialogBinding.btnShopRemoveAds.alpha = 1.0f
            }

            if (prefs.areAllSkinsUnlocked()) {
                dialogBinding.tvPriceAllThemes.text = "UNLOCKED"
                dialogBinding.btnShopAllThemes.alpha = 0.6f
            } else {
                dialogBinding.tvPriceAllThemes.text = "$9.99"
                dialogBinding.btnShopAllThemes.alpha = 1.0f
            }
        }

        updateShopUI()

        // 1. Rewarded Ad: +50 Coins
        dialogBinding.btnShopWatchAd.setOnClickListener {
            soundManager.playButton()
            AdMobManager.getInstance(this).showRewardedCoinsAd(
                activity = this,
                onRewardEarned = {
                    prefs.coins += 50
                    updateDashboard()
                    soundManager.playMatch()
                    soundManager.vibrate(40)
                    Toast.makeText(this, "+50 Coins Granted!", Toast.LENGTH_SHORT).show()
                },
                onAdClosed = {}
            )
        }

        // 2. Remove Ads ($4.99)
        dialogBinding.btnShopRemoveAds.setOnClickListener {
            if (prefs.isAdRemoved) {
                Toast.makeText(this, "Ads are already removed!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            soundManager.playButton()
            BillingManager.getInstance(this).launchPurchaseFlow(this, BillingManager.PRODUCT_REMOVE_ADS) { success, msg ->
                if (success) {
                    Toast.makeText(this, "Ads Removed Forever!", Toast.LENGTH_SHORT).show()
                    updateShopUI()
                    setupBannerAd()
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Unlock All Themes ($9.99)
        dialogBinding.btnShopAllThemes.setOnClickListener {
            if (prefs.areAllSkinsUnlocked()) {
                Toast.makeText(this, "All skins are already unlocked!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            soundManager.playButton()
            BillingManager.getInstance(this).launchPurchaseFlow(this, BillingManager.PRODUCT_ALL_SKINS) { success, msg ->
                if (success) {
                    Toast.makeText(this, "All Themes Unlocked!", Toast.LENGTH_SHORT).show()
                    updateShopUI()
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. Coin Bundles
        fun purchaseCoins(productId: String, amount: Int) {
            soundManager.playButton()
            BillingManager.getInstance(this).launchPurchaseFlow(this, productId) { success, msg ->
                if (success) {
                    updateDashboard()
                    soundManager.playMatch()
                    soundManager.vibrate(40)
                    Toast.makeText(this, "+$amount Coins Added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.btnShopCoins500.setOnClickListener {
            purchaseCoins(BillingManager.PRODUCT_COINS_500, 500)
        }

        dialogBinding.btnShopCoins1500.setOnClickListener {
            purchaseCoins(BillingManager.PRODUCT_COINS_1500, 1500)
        }

        dialogBinding.btnShopCoins5000.setOnClickListener {
            purchaseCoins(BillingManager.PRODUCT_COINS_5000, 5000)
        }

        dialogBinding.btnShopCoins15000.setOnClickListener {
            purchaseCoins(BillingManager.PRODUCT_COINS_15000, 15000)
        }

        dialogBinding.btnCloseShop.setOnClickListener {
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
