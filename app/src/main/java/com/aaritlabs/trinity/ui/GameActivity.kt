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
import com.aaritlabs.trinity.engine.GameEngine
import com.aaritlabs.trinity.engine.LevelGenerator
import com.aaritlabs.trinity.model.Tile

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var prefs: GamePreferences
    private lateinit var soundManager: SoundManager

    private var currentLevelNum = 1
    private lateinit var gameEngine: GameEngine

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

    override fun onResume() {
        super.onResume()
        soundManager.resumeBgm()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBgm()
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

    private fun setupGame() {
        val levelConfig = LevelGenerator.generateLevel(currentLevelNum)
        gameEngine = GameEngine(levelConfig)

        binding.tvGameLevel.text = getString(R.string.level_label, currentLevelNum)
        updateStatsUI()

        binding.tileBoardView.setTiles(gameEngine.allTiles)
        binding.dockTrayView.setDockTiles(gameEngine.dockTiles)

        updateBoosterBadges()
    }

    private fun updateStatsUI() {
        binding.tvGameCoins.text = prefs.coins.toString()
        binding.tvScore.text = getString(R.string.score, gameEngine.score)
        binding.tvTilesRemaining.text = "${gameEngine.remainingTilesCount} tiles left"

        if (gameEngine.combo > 1) {
            binding.tvCombo.visibility = View.VISIBLE
            binding.tvCombo.text = getString(R.string.combo, gameEngine.combo)
            binding.tvCombo.scaleX = 0.5f
            binding.tvCombo.scaleY = 0.5f
            binding.tvCombo.animate()
                .scaleX(1.3f).scaleY(1.3f)
                .setDuration(160)
                .setInterpolator(OvershootInterpolator(2f))
                .withEndAction {
                    binding.tvCombo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }.start()
        } else {
            binding.tvCombo.visibility = View.GONE
        }
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

    private fun handleTileTap(tile: Tile) {
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

        // 2. Determine target slot in dock
        val targetSlotIndex = gameEngine.dockTiles.size.coerceIn(0, binding.dockTrayView.maxSlots - 1)
        val dockPos = IntArray(2)
        binding.dockTrayView.getLocationOnScreen(dockPos)
        val slotBounds = binding.dockTrayView.getSlotBounds(targetSlotIndex)
        val destX = dockPos[0] - overlayPos[0] + slotBounds.left
        val destY = dockPos[1] - overlayPos[1] + slotBounds.top
        val destRect = RectF(destX, destY, destX + slotBounds.width(), destY + slotBounds.height())

        // 3. Mark tile as collected in engine and run fly animation
        when (val result = gameEngine.onTileTapped(tile)) {
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

            is GameEngine.TapResult.Matched -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    // Find indices of matching trio in dock
                    val matchedIndices = mutableSetOf<Int>()
                    for (i in gameEngine.dockTiles.indices) {
                        if (gameEngine.dockTiles[i].type == result.matchedType) {
                            matchedIndices.add(i)
                        }
                    }

                    // Animate match flash & sparkle burst
                    val slotCenter = binding.dockTrayView.getSlotScreenCenter(targetSlotIndex)
                    val burstX = dockPos[0] - overlayPos[0] + slotCenter.x
                    val burstY = dockPos[1] - overlayPos[1] + slotCenter.y
                    binding.flyingTileOverlay.burstSparkles(burstX, burstY)

                    binding.dockTrayView.animateMatch(matchedIndices) {
                        binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                    }

                    if (result.newCombo > 1) {
                        soundManager.playCombo()
                        soundManager.vibrate(60)
                    } else {
                        soundManager.playMatch()
                        soundManager.vibrate(40)
                    }

                    updateStatsUI()
                }
            }

            is GameEngine.TapResult.Victory -> {
                binding.tileBoardView.invalidate()
                binding.flyingTileOverlay.flyTile(tile, startRect, destRect) {
                    binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                    updateStatsUI()

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
                        showVictoryDialog(result.finalScore, result.stars, result.coins)
                    }, 400)
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

    private fun handleUndoBooster() {
        binding.btnBoosterUndo.animate()
            .rotation(-35f)
            .scaleX(0.85f).scaleY(0.85f)
            .setDuration(120)
            .withEndAction {
                binding.btnBoosterUndo.animate().rotation(0f).scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
            }.start()

        if (prefs.boosterUndo > 0) {
            if (gameEngine.undo()) {
                prefs.boosterUndo--
                updateBoosterBadges()
                soundManager.playUndo()
                soundManager.vibrate(35)
                binding.tileBoardView.invalidate()
                binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                updateStatsUI()
            } else {
                Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show()
            }
        } else if (prefs.coins >= 50) {
            if (gameEngine.undo()) {
                prefs.coins -= 50
                soundManager.playUndo()
                soundManager.vibrate(35)
                binding.tileBoardView.invalidate()
                binding.dockTrayView.setDockTiles(gameEngine.dockTiles)
                updateStatsUI()
            }
        } else {
            soundManager.playAlert()
            Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show()
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
            if (gameEngine.shuffle()) {
                prefs.boosterShuffle--
                updateBoosterBadges()
                soundManager.playShuffle()
                soundManager.vibrate(40)
                binding.tileBoardView.animateShuffle {
                    updateStatsUI()
                }
            }
        } else if (prefs.coins >= 50) {
            if (gameEngine.shuffle()) {
                prefs.coins -= 50
                soundManager.playShuffle()
                soundManager.vibrate(40)
                binding.tileBoardView.animateShuffle {
                    updateStatsUI()
                }
            }
        } else {
            soundManager.playAlert()
            Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleWandBooster() {
        binding.btnBoosterWand.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(140)
            .withEndAction {
                binding.btnBoosterWand.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
            }.start()

        if (prefs.boosterWand > 0 || prefs.coins >= 50) {
            val trio = gameEngine.findHintTrio()
            if (trio != null) {
                if (prefs.boosterWand > 0) {
                    prefs.boosterWand--
                } else {
                    prefs.coins -= 50
                }
                updateBoosterBadges()
                soundManager.playWand()
                soundManager.vibrate(45)

                binding.tileBoardView.highlightTiles(trio.map { it.id }.toSet())
                updateStatsUI()
            } else {
                Toast.makeText(this, "No obvious matches found!", Toast.LENGTH_SHORT).show()
            }
        } else {
            soundManager.playAlert()
            Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVictoryDialog(finalScore: Int, stars: Int, coins: Int) {
        val dialogBinding = DialogVictoryBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
        }

        dialogBinding.tvVicScore.text = "Final Score: 0"
        dialogBinding.tvVicCoins.text = "+$coins Coins"

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

        // Animated Score Counter
        val scoreAnim = ValueAnimator.ofInt(0, finalScore).apply {
            duration = 800
            addUpdateListener {
                dialogBinding.tvVicScore.text = "Final Score: ${it.animatedValue}"
            }
        }
        scoreAnim.start()

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

        dialogBinding.btnVicNextLevel.setOnClickListener {
            dialog.dismiss()
            soundManager.playButton()
            if (currentLevelNum < 1000) {
                currentLevelNum++
                setupGame()
            } else {
                finish()
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
        val dialogBinding = DialogPauseBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.DialogAnimation)
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
