package com.aaritlabs.trinity.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
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
import com.aaritlabs.trinity.R
import com.aaritlabs.trinity.audio.SoundManager
import com.aaritlabs.trinity.data.GamePreferences
import com.aaritlabs.trinity.databinding.ActivityGameBinding
import com.aaritlabs.trinity.databinding.DialogDefeatBinding
import com.aaritlabs.trinity.databinding.DialogPauseBinding
import com.aaritlabs.trinity.databinding.DialogVictoryBinding
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.animation.doOnEnd
import com.aaritlabs.trinity.ads.AdMobManager
import com.aaritlabs.trinity.databinding.DialogGetBoosterBinding
import com.aaritlabs.trinity.databinding.DialogWatchAdBinding
import java.util.Locale
import com.aaritlabs.trinity.engine.GameEngine
import com.aaritlabs.trinity.engine.LevelGenerator
import com.aaritlabs.trinity.model.Tile

enum class BoosterType {
    UNDO, SHUFFLE, HINT
}

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var prefs: GamePreferences
    private lateinit var soundManager: SoundManager

    private var currentLevelNum = 1
    private lateinit var gameEngine: GameEngine
    private var isMatchAnimating = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed && ::gameEngine.isInitialized) {
                updateTimerDisplay()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = GamePreferences(this)
        soundManager = SoundManager.getInstance(this)

        currentLevelNum = intent.getIntExtra("LEVEL_NUM", prefs.currentLevel)

        setupSafeAreaInsets()
        setupGame()
        setupListeners()
    }

    private fun setupSafeAreaInsets() {
        val baseTopPadding = binding.gameTopBar.paddingTop
        val baseBottomMargin = (binding.llBoosters.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            val cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            val topInset = maxOf(systemBars.top, cutoutTop)

            binding.gameTopBar.updatePadding(
                left = systemBars.left + 14,
                top = topInset + 14,
                right = systemBars.right + 14
            )

            binding.llBoosters.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = baseBottomMargin + systemBars.bottom
            }

            insets
        }
    }

    private var lastShownCoins = -1

    private fun setupGame() {
        val levelConfig = LevelGenerator.generateLevel(currentLevelNum)
        gameEngine = GameEngine(levelConfig)
        lastShownCoins = 0

        binding.tvGameLevel.text = getString(R.string.level_label, currentLevelNum)
        updateStatsUI()

        binding.tileBoardView.setTiles(gameEngine.allTiles)
        binding.dockTrayView.setDockTiles(gameEngine.dockTiles)

        updateBoosterBadges()

        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)
    }

    private fun updateTimerDisplay() {
        if (::gameEngine.isInitialized) {
            val elapsed = gameEngine.elapsedSeconds
            val mins = elapsed / 60
            val secs = elapsed % 60
            val timeStr = String.format(Locale.US, "%02d:%02d", mins, secs)
            val coinsGained = gameEngine.coinsEarned
            binding.tvTilesRemaining.text = "${gameEngine.remainingTilesCount} tiles left • ⏱ $timeStr • 🪙 +$coinsGained"

            if (lastShownCoins != -1 && coinsGained > lastShownCoins) {
                binding.tvTilesRemaining.animate()
                    .scaleX(1.12f).scaleY(1.12f)
                    .setDuration(120)
                    .withEndAction {
                        binding.tvTilesRemaining.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
                    }.start()
            }
            lastShownCoins = coinsGained
        }
    }

    private fun updateStatsUI() {
        binding.tvGameCoins.text = prefs.coins.toString()
        updateTimerDisplay()

        if (gameEngine.combo > 1) {
            binding.llComboBanner.visibility = View.VISIBLE
            binding.tvCombo.text = getString(R.string.combo, gameEngine.combo)
            binding.llComboBanner.scaleX = 0.6f
            binding.llComboBanner.scaleY = 0.6f
            binding.llComboBanner.animate()
                .scaleX(1.15f).scaleY(1.15f)
                .setDuration(160)
                .setInterpolator(OvershootInterpolator(2f))
                .withEndAction {
                    binding.llComboBanner.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }.start()
        } else {
            binding.llComboBanner.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBgm()
        timerHandler.removeCallbacks(timerRunnable)
        if (::gameEngine.isInitialized) {
            gameEngine.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager.resumeBgm()
        if (::gameEngine.isInitialized) {
            gameEngine.onResume()
            timerHandler.post(timerRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateBoosterBadges() {
        binding.tvBadgeUndo.text = prefs.boosterUndo.toString()
        binding.tvBadgeShuffle.text = prefs.boosterShuffle.toString()
        binding.tvBadgeWand.text = prefs.boosterWand.toString()
    }

    private fun setupListeners() {
        binding.tileBoardView.onTileClickListener = { tile ->
            handleTileTap(tile)
        }

        binding.tileBoardView.onBlockedTileClickListener = {
            soundManager.playAlert()
            soundManager.vibrate(20)
        }

        binding.btnPause.setOnClickListener {
            soundManager.playButton()
            soundManager.vibrate(25)
            showPauseDialog()
        }

        binding.btnBoosterUndo.setOnClickListener {
            handleUndoBooster()
        }

        binding.btnBoosterShuffle.setOnClickListener {
            handleShuffleBooster()
        }

        binding.btnBoosterWand.setOnClickListener {
            handleWandBooster()
        }
    }

    private fun handleTileTap(tile: Tile, force: Boolean = false) {
        if (!force && isMatchAnimating) return

        soundManager.playClick()
        soundManager.vibrate(30)

        // 1. Calculate tile coordinates on screen
        val boardPos = IntArray(2)
        binding.tileBoardView.getLocationOnScreen(boardPos)
        val tileBoardRect = binding.tileBoardView.getTilePositionOnScreen(tile)

        val overlayPos = IntArray(2)
        binding.flyingTileOverlay.getLocationOnScreen(overlayPos)

        val startX = boardPos[0] - overlayPos[0] + tileBoardRect.left
        val startY = boardPos[1] - overlayPos[1] + tileBoardRect.top
        val startRect = RectF(startX, startY, startX + tileBoardRect.width(), startY + tileBoardRect.height())

        // 2. Determine target slot in dock (grouping matching tiles together)
        val targetSlotIndex = gameEngine.getInsertIndexFor(tile).coerceIn(0, binding.dockTrayView.maxSlots - 1)
        val dockPos = IntArray(2)
        binding.dockTrayView.getLocationOnScreen(dockPos)
        val slotBounds = binding.dockTrayView.getSlotBounds(targetSlotIndex)
        val destX = dockPos[0] - overlayPos[0] + slotBounds.left
        val destY = dockPos[1] - overlayPos[1] + slotBounds.top
        val destRect = RectF(destX, destY, destX + slotBounds.width(), destY + slotBounds.height())

        // 3. Mark tile as collected in engine and run fly animation
        when (val result = gameEngine.onTileTapped(tile, force)) {
            is GameEngine.TapResult.PlacedInDock -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles, targetSlotIndex)
                    updateStatsUI()

                    if (gameEngine.dockTiles.size >= gameEngine.maxDockCapacity - 1) {
                        soundManager.playAlert()
                    }
                }
            }

            is GameEngine.TapResult.PairFormed -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    // Update tray with pair inserted
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                    val primaryColor = com.aaritlabs.trinity.model.TileThemeHelper.getPrimaryColor(result.tile.type)
                    // Distinct 2-tile magnetic clash, tilt & pulse animation with themed aura
                    binding.dockTrayView.animatePairFormed(result.pairIndices, primaryColor)

                    // Explosive clash: shockwave ring + themed 4-point sparkle stars
                    if (result.pairIndices.size >= 2) {
                        val center1 = binding.dockTrayView.getSlotScreenCenter(result.pairIndices[0])
                        val center2 = binding.dockTrayView.getSlotScreenCenter(result.pairIndices[1])
                        val seamX = dockPos[0] - overlayPos[0] + (center1.x + center2.x) / 2f
                        val seamY = dockPos[1] - overlayPos[1] + (center1.y + center2.y) / 2f
                        binding.flyingTileOverlay.burstPairClash(seamX, seamY, result.tile.type)
                    }

                    soundManager.playClick()
                    soundManager.vibrate(35)
                    updateStatsUI()

                    if (gameEngine.dockTiles.size >= gameEngine.maxDockCapacity - 1) {
                        soundManager.playAlert()
                    }
                }
            }

            is GameEngine.TapResult.Matched -> {
                isMatchAnimating = true
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    // Display dock with all 3 tiles of the trio visible side-by-side
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles)

                    val centerSlotIndex = if (result.trioIndices.isNotEmpty()) {
                        result.trioIndices[result.trioIndices.size / 2]
                    } else {
                        targetSlotIndex
                    }
                    val slotCenter = binding.dockTrayView.getSlotScreenCenter(centerSlotIndex)
                    val burstX = dockPos[0] - overlayPos[0] + slotCenter.x
                    val burstY = dockPos[1] - overlayPos[1] + slotCenter.y

                    val primaryColor = com.aaritlabs.trinity.model.TileThemeHelper.getPrimaryColor(result.matchedType)
                    // Grand celebration: Sunburst nova + double shockwaves + giant sparkle stars + floating score!
                    binding.flyingTileOverlay.burstMatchClash(burstX, burstY, result.matchedType, result.newCombo, result.points)

                    if (result.newCombo > 1) {
                        soundManager.playCombo()
                        soundManager.vibrate(60)
                    } else {
                        soundManager.playMatch()
                        soundManager.vibrate(40)
                    }

                    // Distinct 3-tile celebration & blast animation with themed aura
                    binding.dockTrayView.animateMatch(result.trioIndices.toSet(), primaryColor) {
                        gameEngine.removeMatchedTrio(result.clearedTiles)
                        binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                        isMatchAnimating = false
                        updateStatsUI()

                        if (result.isVictory) {
                            handleVictorySequence(gameEngine.getVictoryData())
                        }
                    }

                    updateStatsUI()
                }
            }

            is GameEngine.TapResult.Victory -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                    updateStatsUI()
                    handleVictorySequence(result)
                }
            }

            is GameEngine.TapResult.GameOver -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                    updateStatsUI()

                    soundManager.playFail()
                    soundManager.vibrate(80)

                    Handler(Looper.getMainLooper()).postDelayed({
                        showDefeatDialog()
                    }, 350)
                }
            }

            GameEngine.TapResult.Ignored -> {
                soundManager.playAlert()
                soundManager.vibrate(25)
            }
        }
    }

    private fun handleVictorySequence(result: GameEngine.TapResult.Victory) {
        soundManager.playWin()
        soundManager.vibrate(100)

        // Sparkle celebration
        val cx = binding.flyingTileOverlay.width / 2f
        val cy = binding.flyingTileOverlay.height / 2f
        binding.flyingTileOverlay.burstSparkles(cx, cy)
        binding.flyingTileOverlay.burstSparkles(cx - 100f, cy + 60f)
        binding.flyingTileOverlay.burstSparkles(cx + 100f, cy + 60f)

        // Save progression
        prefs.coins += result.coins
        prefs.setStarsForLevel(currentLevelNum, result.stars)
        prefs.unlockNextLevel(currentLevelNum)

        Handler(Looper.getMainLooper()).postDelayed({
            showVictoryDialog(result.finalScore, result.stars, result.coins, result.elapsedSeconds)
        }, 400)
    }

    private fun handleUndoBooster() {
        binding.btnBoosterUndo.animate()
            .rotation(-35f)
            .scaleX(0.85f).scaleY(0.85f)
            .setDuration(120)
            .withEndAction {
                binding.btnBoosterUndo.animate().rotation(0f).scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
            }.start()

        if (prefs.boosterUndo > 0) {
            if (executeUndoBooster()) {
                prefs.boosterUndo--
                updateBoosterBadges()
            }
        } else {
            showGetBoosterDialog(BoosterType.UNDO)
        }
    }

    private fun executeUndoBooster(): Boolean {
        if (gameEngine.undo()) {
            soundManager.playUndo()
            soundManager.vibrate(35)
            binding.tileBoardView.invalidate()
            binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
            updateStatsUI()
            return true
        } else {
            Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun handleShuffleBooster() {
        binding.btnBoosterShuffle.animate()
            .rotationBy(360f)
            .scaleX(1.15f).scaleY(1.15f)
            .setDuration(350)
            .withEndAction {
                binding.btnBoosterShuffle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

        if (prefs.boosterShuffle > 0) {
            if (executeShuffleBooster()) {
                prefs.boosterShuffle--
                updateBoosterBadges()
            }
        } else {
            showGetBoosterDialog(BoosterType.SHUFFLE)
        }
    }

    private fun executeShuffleBooster(): Boolean {
        if (gameEngine.shuffle()) {
            soundManager.playShuffle()
            soundManager.vibrate(40)
            binding.tileBoardView.animateShuffle {
                updateStatsUI()
            }
            return true
        } else {
            Toast.makeText(this, "Cannot shuffle now!", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun handleWandBooster() {
        binding.btnBoosterWand.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(140)
            .withEndAction {
                binding.btnBoosterWand.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
            }.start()

        if (prefs.boosterWand > 0) {
            prefs.boosterWand--
            updateBoosterBadges()
            executeHintBooster()
        } else {
            showGetBoosterDialog(BoosterType.HINT)
        }
    }

    private fun executeHintBooster() {
        val boardTilesToCollect = gameEngine.findHintTrio()
        if (boardTilesToCollect.isNullOrEmpty()) {
            Toast.makeText(this, "No obvious matches found!", Toast.LENGTH_SHORT).show()
            return
        }

        soundManager.playWand()
        soundManager.vibrate(45)

        val boardTileIds = boardTilesToCollect.map { it.id }.toSet()
        binding.tileBoardView.highlightTiles(boardTileIds)

        // Magic sparkle burst at each tile position
        val overlayPos = IntArray(2)
        binding.flyingTileOverlay.getLocationOnScreen(overlayPos)
        val boardPos = IntArray(2)
        binding.tileBoardView.getLocationOnScreen(boardPos)

        for (tile in boardTilesToCollect) {
            val r = binding.tileBoardView.getTilePositionOnScreen(tile)
            val cx = boardPos[0] - overlayPos[0] + r.centerX()
            val cy = boardPos[1] - overlayPos[1] + r.centerY()
            binding.flyingTileOverlay.burstSparkles(cx, cy)
        }

        // Staggered animated collection into the dock (force unblocks tiles under layers)
        for (i in boardTilesToCollect.indices) {
            val tile = boardTilesToCollect[i]
            binding.root.postDelayed({
                if (!tile.isCollected && !isFinishing && !isDestroyed) {
                    handleTileTap(tile, force = true)
                }
            }, (i * 240).toLong())
        }
    }

    private fun showGetBoosterDialog(type: BoosterType) {
        val dialogBinding = DialogGetBoosterBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        val name = when (type) {
            BoosterType.UNDO -> "Undo"
            BoosterType.SHUFFLE -> "Shuffle"
            BoosterType.HINT -> "Hint"
        }

        val iconRes = when (type) {
            BoosterType.UNDO -> R.drawable.ic_undo
            BoosterType.SHUFFLE -> R.drawable.ic_shuffle
            BoosterType.HINT -> R.drawable.ic_wand
        }

        dialogBinding.ivBoosterHeaderIcon.setImageResource(iconRes)
        dialogBinding.tvBoosterTitle.text = "Need More $name?"
        dialogBinding.tvBoosterSubtitle.text = "You have 0 $name left. Choose an option to get +1:"

        val adsRemaining = when (type) {
            BoosterType.UNDO -> prefs.adsRemainingUndo
            BoosterType.SHUFFLE -> prefs.adsRemainingShuffle
            BoosterType.HINT -> prefs.adsRemainingWand
        }

        dialogBinding.tvCoinsBalance.text = "Your balance: ${prefs.coins} Coins"

        // Watch Ad Option
        if (adsRemaining > 0) {
            dialogBinding.tvWatchAdSubtitle.text = "Free • $adsRemaining / 5 left today"
            dialogBinding.tvWatchAdSubtitle.setTextColor(Color.parseColor("#10B981"))
            dialogBinding.tvAdActionBadge.text = "FREE"
            dialogBinding.tvAdActionBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            dialogBinding.btnOptionWatchAd.alpha = 1.0f
            dialogBinding.btnOptionWatchAd.setOnClickListener {
                dialog.dismiss()
                showWatchAdSimulation(type)
            }
        } else {
            dialogBinding.tvWatchAdSubtitle.text = "Daily limit reached (0 / 5 left)"
            dialogBinding.tvWatchAdSubtitle.setTextColor(Color.parseColor("#94A3B8"))
            dialogBinding.tvAdActionBadge.text = "LIMIT"
            dialogBinding.tvAdActionBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
            dialogBinding.btnOptionWatchAd.alpha = 0.6f
            dialogBinding.btnOptionWatchAd.setOnClickListener {
                soundManager.playAlert()
                Toast.makeText(this, "Daily video ad limit reached for $name (5/5 used today)!", Toast.LENGTH_SHORT).show()
            }
        }

        // Coins Option (200 Coins)
        dialogBinding.btnOptionCoins.setOnClickListener {
            if (prefs.coins >= 200) {
                prefs.coins -= 200
                updateStatsUI()
                dialog.dismiss()
                soundManager.playButton()
                soundManager.vibrate(30)
                Toast.makeText(this, "Used 200 Coins for +1 $name!", Toast.LENGTH_SHORT).show()
                executeBoosterAfterReplenish(type)
            } else {
                soundManager.playAlert()
                soundManager.vibrate(25)
                Toast.makeText(this, "Not enough coins! You need 200 coins.", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCloseGetBooster.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dialog.window?.setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showWatchAdSimulation(type: BoosterType) {
        val name = when (type) {
            BoosterType.UNDO -> "Undo"
            BoosterType.SHUFFLE -> "Shuffle"
            BoosterType.HINT -> "Hint"
        }

        fun grantReward() {
            when (type) {
                BoosterType.UNDO -> prefs.adsRemainingUndo--
                BoosterType.SHUFFLE -> prefs.adsRemainingShuffle--
                BoosterType.HINT -> prefs.adsRemainingWand--
            }
            soundManager.playMatch()
            soundManager.vibrate(50)
            Toast.makeText(this@GameActivity, "Ad watched! +1 $name granted!", Toast.LENGTH_SHORT).show()
            executeBoosterAfterReplenish(type)
        }

        AdMobManager.getInstance(this).showRewardedBoosterAd(
            activity = this,
            onRewardEarned = {
                grantReward()
            },
            onAdClosed = {
                // Ad closed
            },
            onFallbackSimulation = {
                runFallbackAdDialog(name) {
                    grantReward()
                }
            }
        )
    }

    private fun runFallbackAdDialog(name: String, onFinished: () -> Unit) {
        val adBinding = DialogWatchAdBinding.inflate(LayoutInflater.from(this))
        val adDialog = AlertDialog.Builder(this)
            .setView(adBinding.root)
            .setCancelable(false)
            .create()

        adDialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        adBinding.tvAdHeading.text = "Watching Video Ad for $name"
        adDialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.85f).toInt()
        adDialog.window?.setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        val anim = ValueAnimator.ofInt(0, 100).apply {
            duration = 2000
            addUpdateListener {
                val progress = it.animatedValue as Int
                adBinding.pbAdProgress.progress = progress
                val remainingSec = ((100 - progress) * 2 / 100.0).coerceAtLeast(0.0)
                adBinding.tvAdCountdown.text = String.format(Locale.US, "Reward in %.1fs", remainingSec)
            }
            doOnEnd {
                if (!isFinishing && !isDestroyed) {
                    adDialog.dismiss()
                    onFinished()
                }
            }
        }
        anim.start()
    }

    private fun executeBoosterAfterReplenish(type: BoosterType) {
        when (type) {
            BoosterType.UNDO -> executeUndoBooster()
            BoosterType.SHUFFLE -> executeShuffleBooster()
            BoosterType.HINT -> executeHintBooster()
        }
        updateBoosterBadges()
        updateStatsUI()
    }

    private fun showVictoryDialog(finalScore: Int, stars: Int, coins: Int, elapsedSeconds: Long) {
        val dialogBinding = DialogVictoryBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.tvVicCoins.text = "+$coins Coins"

        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        val timeLabel = when (stars) {
            3 -> "(⭐⭐⭐ Under 2 mins)"
            2 -> "(⭐⭐ Under 6 mins)"
            else -> "(⭐ Over 6 mins)"
        }
        dialogBinding.tvVicTime.text = String.format(Locale.US, "Time: %02d:%02d  %s", mins, secs, timeLabel)

        // Star views
        val gold = ContextCompat.getColor(this, R.color.accent_gold)
        val inactive = ContextCompat.getColor(this, R.color.star_inactive)
        dialogBinding.ivVicStar1.setColorFilter(inactive)
        dialogBinding.ivVicStar2.setColorFilter(inactive)
        dialogBinding.ivVicStar3.setColorFilter(inactive)

        dialogBinding.ivVicStar1.scaleX = 0f
        dialogBinding.ivVicStar1.scaleY = 0f
        dialogBinding.ivVicStar2.scaleX = 0f
        dialogBinding.ivVicStar2.scaleY = 0f
        dialogBinding.ivVicStar3.scaleX = 0f
        dialogBinding.ivVicStar3.scaleY = 0f

        // Trophy bounce animation
        dialogBinding.ivVictoryTrophy.animate()
            .scaleX(1.18f).scaleY(1.18f)
            .setDuration(400)
            .withEndAction {
                dialogBinding.ivVictoryTrophy.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            }.start()

        // Sequential Star Pop Animations
        val starViews = listOf(dialogBinding.ivVicStar1, dialogBinding.ivVicStar2, dialogBinding.ivVicStar3)
        for (i in 0 until stars.coerceIn(0, 3)) {
            val starView = starViews[i]
            starView.postDelayed({
                starView.setColorFilter(gold)
                starView.animate()
                    .scaleX(1.25f).scaleY(1.25f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .withEndAction {
                        starView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
                    }.start()
                soundManager.playClick()
                soundManager.vibrate(20)
            }, (250 + i * 280).toLong())
        }

        // Show interstitial ad only every 3 levels completed
        prefs.completedLevelsCount++
        val shouldShowAd = !prefs.isAdRemoved && (prefs.completedLevelsCount % 3 == 0)

        dialogBinding.btnVicNextLevel.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()

            fun proceed() {
                if (currentLevelNum < 1000) {
                    currentLevelNum++
                    setupGame()
                } else {
                    finish()
                }
            }

            if (shouldShowAd) {
                AdMobManager.getInstance(this).showInterstitialAd(this) {
                    proceed()
                }
            } else {
                proceed()
            }
        }

        dialogBinding.btnVicReplay.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            setupGame()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
        dialog.window?.setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showDefeatDialog() {
        val dialogBinding = DialogDefeatBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        // Wobble icon
        dialogBinding.ivDefeatIcon.animate()
            .rotation(-10f)
            .setDuration(120)
            .withEndAction {
                dialogBinding.ivDefeatIcon.animate().rotation(10f).setDuration(120).withEndAction {
                    dialogBinding.ivDefeatIcon.animate().rotation(0f).setDuration(100).start()
                }.start()
            }.start()

        dialogBinding.btnDefeatRevive.setOnClickListener {
            if (prefs.coins >= 100) {
                prefs.coins -= 100
                dialog.dismiss()
                soundManager.playUndo()
                repeat(3) { gameEngine.undo() }
                binding.tileBoardView.invalidate()
                binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                updateStatsUI()
                Toast.makeText(this, "Revived! 3 tiles cleared from dock", Toast.LENGTH_SHORT).show()
            } else {
                soundManager.playAlert()
                Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnDefeatRetry.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            setupGame()
        }

        dialogBinding.btnDefeatHome.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            finish()
        }

        dialog.show()
        val dialogWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
        dialog.window?.setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showPauseDialog() {
        if (::gameEngine.isInitialized) {
            gameEngine.onPause()
        }
        val dialogBinding = DialogPauseBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialog.setOnDismissListener {
            if (::gameEngine.isInitialized) {
                gameEngine.onResume()
            }
        }

        dialogBinding.btnPauseResume.setOnClickListener {
            soundManager.playButton()
            dialog.dismiss()
        }

        dialogBinding.btnPauseRestart.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            setupGame()
        }

        dialogBinding.btnPauseHome.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            finish()
        }

        dialog.show()
        val pauseWidth = (resources.displayMetrics.widthPixels * 0.74f).toInt()
        dialog.window?.setLayout(pauseWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
