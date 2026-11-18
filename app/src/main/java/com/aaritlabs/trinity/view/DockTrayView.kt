package com.aaritlabs.trinity.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import com.aaritlabs.trinity.data.ItemSkinManager
import com.aaritlabs.trinity.model.Tile
import com.aaritlabs.trinity.model.TileType

class DockTrayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val maxSlots = 7
    private var tiles: List<Tile> = emptyList()

    private val trayShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val slotEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    // Tile paints
    private val tileBasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileFacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val matchGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFD700")
    }
    private val pairGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.parseColor("#00E5FF")
    }

    private val trayBounds = RectF()
    private val slotBounds = RectF()
    private val faceBounds = RectF()

    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Flash/Match-3 animation
    private var matchingIndices: Set<Int> = emptySet()
    private var matchAnimScale: Float = 1.0f
    private var matchAnimOffsetY: Float = 0f
    private var matchAnimRotation: Float = 0f
    private var matchClashShift: Float = 0f
    private var matchGlowAlpha: Int = 0
    private var matchWhiteFlashAlpha: Int = 0
    private val whiteFlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Pair Formed animation
    private var pairIndices: List<Int> = emptyList()
    private var pairScale: Float = 1.0f
    private var pairTilt: Float = 0f
    private var pairClashShift: Float = 0f
    private var pairGlowAlpha: Int = 0
    private var pairAnimator: ValueAnimator? = null

    // Newly inserted slot bounce animation
    private var newlyInsertedIndex: Int? = null
    private var insertBounceScale: Float = 1.0f

    init {
        updateThemePaints()
    }

    private fun isDarkMode(): Boolean {
        val prefs = com.aaritlabs.trinity.data.GamePreferences(context)
        return when (prefs.themeMode) {
            com.aaritlabs.trinity.data.GamePreferences.THEME_DARK -> true
            com.aaritlabs.trinity.data.GamePreferences.THEME_LIGHT -> false
            else -> {
                val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun updateThemePaints() {
        val dark = isDarkMode()
        if (dark) {
            trayShadowPaint.color = Color.parseColor("#80000000")
            trayPaint.color = Color.parseColor("#181E2E")
            trayBorderPaint.color = Color.parseColor("#FFD54F")
            slotEmptyPaint.color = Color.parseColor("#0D1220")
            slotStrokePaint.color = Color.parseColor("#2C354D")
            tileBasePaint.color = Color.parseColor("#475569")
            tileFacePaint.color = Color.parseColor("#FFFFFF")
            tileStrokePaint.color = Color.parseColor("#334155")
        } else {
            // Light Theme: Luminous Frosted Glass Frame with Warm Gold Border & Soft Inset Slots
            trayShadowPaint.color = Color.parseColor("#22000000")
            trayPaint.color = Color.parseColor("#EAFFFFFF") // 92% luminous frosted white
            trayBorderPaint.color = Color.parseColor("#FFD54F") // warm gold trim
            slotEmptyPaint.color = Color.parseColor("#12000000") // soft 7% inset recess
            slotStrokePaint.color = Color.parseColor("#22000000") // subtle 13% inner bezel border
            tileBasePaint.color = Color.parseColor("#CBD5E1")
            tileFacePaint.color = Color.parseColor("#FFFFFF")
            tileStrokePaint.color = Color.parseColor("#E2E8F0")
        }
    }

    fun setDockTiles(dockTiles: List<Tile>, animateInsertIndex: Int? = null) {
        this.tiles = dockTiles.take(maxSlots)
        if (animateInsertIndex != null && animateInsertIndex in 0 until maxSlots) {
            newlyInsertedIndex = animateInsertIndex
            val anim = ValueAnimator.ofFloat(0.6f, 1.15f, 1.0f).apply {
                duration = 200
                interpolator = OvershootInterpolator()
                addUpdateListener {
                    insertBounceScale = it.animatedValue as Float
                    invalidate()
                }
            }
            anim.start()
        } else {
            newlyInsertedIndex = null
            invalidate()
        }
    }

    /**
     * Distinct clash animation when 2nd matching tile is added:
     * - Physical clash: both tiles bump together laterally at the seam (+/- 7.5px)
     * - Satisfying elastic spring bounce back to position
     * - Tilt toward each other like magnetic attraction (+/- 5.5 deg)
     * - Luminous theme aura (e.g. ruby red for rose) pulses around both tiles!
     */
    fun animatePairFormed(indices: List<Int>, themeColor: Int = Color.parseColor("#00E5FF")) {
        if (indices.size < 2) return
        val dp = resources.displayMetrics.density
        pairAnimator?.cancel()
        pairIndices = indices
        pairGlowPaint.color = themeColor

        pairAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                // Physical clash shift (bump towards each other at f=0.25, then spring back)
                pairClashShift = if (f < 0.25f) {
                    (f / 0.25f) * 8.5f * dp
                } else {
                    val t = (f - 0.25f) / 0.75f
                    8.5f * dp * (1.0f - t) * kotlin.math.cos(t * Math.PI * 2.5).toFloat()
                }

                // Scale pulse: 1.0 -> 1.22 at impact (f=0.28) -> 1.0
                pairScale = if (f < 0.28f) {
                    1.0f + (f / 0.28f) * 0.22f
                } else {
                    1.22f - ((f - 0.28f) / 0.72f) * 0.22f
                }

                // Tilt toward each other: 0 -> 6 deg -> 0
                pairTilt = if (f < 0.28f) {
                    (f / 0.28f) * 6.0f
                } else {
                    (1.0f - ((f - 0.28f) / 0.72f)) * 6.0f
                }

                // Themed glow alpha: flares up to 235 at clash impact, then dissolves smoothly
                pairGlowAlpha = if (f < 0.25f) {
                    ((f / 0.25f) * 235).toInt()
                } else {
                    ((1.0f - ((f - 0.25f) / 0.75f)) * 235).toInt().coerceIn(0, 255)
                }

                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    pairIndices = emptyList()
                    pairScale = 1.0f
                    pairTilt = 0f
                    pairClashShift = 0f
                    pairGlowAlpha = 0
                    invalidate()
                }
            })
            start()
        }
    }

    /**
     * Attention-Grabbing Grand Celebration when 3rd matching tile is added:
     * - Stage 1 (0 to 42%): Massive jump up (-22dp) and clash inward with white energy flash!
     * - Physical tray punch vibration
     * - Stage 2 (42% to 100%): Radiant theme-colored burst with intense halo, dissolved and shrunk to 0 as remaining tiles slide in.
     */
    fun animateMatch(indices: Set<Int>, themeColor: Int = Color.parseColor("#FFFFD700"), onEnd: () -> Unit) {
        matchingIndices = indices
        matchGlowPaint.color = themeColor
        val dp = resources.displayMetrics.density

        // Tray physical punch-shake
        animate().cancel()
        animate()
            .translationY(dp * 5f)
            .setDuration(45)
            .withEndAction {
                animate()
                    .translationY(-dp * 4f)
                    .setDuration(45)
                    .withEndAction {
                        animate().translationY(0f).setDuration(60).start()
                    }.start()
            }.start()

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                if (f < 0.42f) {
                    val t = f / 0.42f
                    matchAnimScale = 1.0f + t * 0.35f
                    matchAnimOffsetY = -22f * dp * t
                    matchAnimRotation = 9.0f * t
                    matchClashShift = 8.0f * dp * t
                    matchGlowAlpha = (t * 255).toInt().coerceIn(0, 255)
                    matchWhiteFlashAlpha = ((1.0f - t) * 220).toInt().coerceIn(0, 255)
                } else {
                    val t = (f - 0.42f) / 0.58f
                    matchAnimScale = (1.35f * (1.0f - t)).coerceAtLeast(0f)
                    matchAnimOffsetY = -22f * dp * (1.0f - t)
                    matchAnimRotation = 9.0f * (1.0f - t)
                    matchClashShift = 8.0f * dp * (1.0f - t)
                    matchGlowAlpha = ((1.0f - t) * 255).toInt().coerceIn(0, 255)
                    matchWhiteFlashAlpha = 0
                }
                invalidate()
            }
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                matchingIndices = emptySet()
                matchAnimScale = 1.0f
                matchAnimOffsetY = 0f
                matchAnimRotation = 0f
                matchClashShift = 0f
                matchGlowAlpha = 0
                matchWhiteFlashAlpha = 0
                onEnd()
            }
        })
        anim.start()
    }

    fun getSlotScreenCenter(index: Int): PointF {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return PointF(0f, 0f)

        val trayPadding = 8f
        val innerW = (w - 2 * trayPadding) - 20f
        val slotSpacing = 6f
        val totalSpacing = (maxSlots - 1) * slotSpacing
        val slotW = (innerW - totalSpacing) / maxSlots
        val slotH = (h - 2 * trayPadding) - 16f
        val startX = trayPadding + 10f
        val startY = trayPadding + 8f

        val sx = startX + index * (slotW + slotSpacing)
        return PointF(sx + slotW / 2f, startY + slotH / 2f)
    }

    fun getSlotBounds(index: Int): RectF {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return RectF(0f, 0f, 0f, 0f)

        val trayPadding = 8f
        val innerW = (w - 2 * trayPadding) - 20f
        val slotSpacing = 6f
        val totalSpacing = (maxSlots - 1) * slotSpacing
        val slotW = (innerW - totalSpacing) / maxSlots
        val slotH = (h - 2 * trayPadding) - 16f
        val startX = trayPadding + 10f
        val startY = trayPadding + 8f

        val sx = startX + index * (slotW + slotSpacing)
        return RectF(sx, startY, sx + slotW, startY + slotH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        updateThemePaints()

        // 1. Draw Drop Shadow & Tray Frame
        val trayPadding = 8f
        trayBounds.set(trayPadding, trayPadding, w - trayPadding, h - trayPadding)
        val trayCorner = 20f
        val shadowBounds = RectF(trayBounds.left + 2f, trayBounds.top + 4f, trayBounds.right + 2f, trayBounds.bottom + 6f)
        canvas.drawRoundRect(shadowBounds, trayCorner, trayCorner, trayShadowPaint)
        canvas.drawRoundRect(trayBounds, trayCorner, trayCorner, trayPaint)
        canvas.drawRoundRect(trayBounds, trayCorner, trayCorner, trayBorderPaint)

        // 2. Draw 7 Inset Slots
        val slotSpacing = 6f
        val innerW = trayBounds.width() - 20f
        val totalSpacing = (maxSlots - 1) * slotSpacing
        val slotW = (innerW - totalSpacing) / maxSlots
        val slotH = trayBounds.height() - 16f
        val startX = trayBounds.left + 10f
        val startY = trayBounds.top + 8f

        val slotCorner = 12f
        val sortedMatching = matchingIndices.sorted()

        for (i in 0 until maxSlots) {
            val sx = startX + i * (slotW + slotSpacing)
            slotBounds.set(sx, startY, sx + slotW, startY + slotH)
            canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, slotEmptyPaint)
            canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, slotStrokePaint)

            // If this slot is occupied by a tile
            if (i < tiles.size) {
                val tile = tiles[i]
                val isMatching = matchingIndices.contains(i)
                val isPair = pairIndices.contains(i)
                val isNewlyInserted = (i == newlyInsertedIndex)

                val scale = when {
                    isMatching -> matchAnimScale
                    isPair -> pairScale
                    isNewlyInserted -> insertBounceScale
                    else -> 1.0f
                }

                val offsetX = when {
                    isPair && pairIndices.size >= 2 -> {
                        if (i == pairIndices.first()) pairClashShift else -pairClashShift
                    }
                    isMatching && sortedMatching.size == 3 -> {
                        when (i) {
                            sortedMatching[0] -> matchClashShift
                            sortedMatching[1] -> 0f
                            sortedMatching[2] -> -matchClashShift
                            else -> 0f
                        }
                    }
                    else -> 0f
                }
                val offsetY = if (isMatching) matchAnimOffsetY else 0f

                val rotation = when {
                    isMatching && sortedMatching.size == 3 -> {
                        when (i) {
                            sortedMatching[0] -> -matchAnimRotation
                            sortedMatching[1] -> 0f
                            sortedMatching[2] -> matchAnimRotation
                            else -> 0f
                        }
                    }
                    isPair && pairIndices.size >= 2 -> {
                        if (i == pairIndices.first()) pairTilt else -pairTilt
                    }
                    else -> 0f
                }

                canvas.save()
                val cx = slotBounds.centerX()
                val cy = slotBounds.centerY()

                canvas.translate(offsetX, offsetY)
                if (scale != 1.0f) {
                    canvas.scale(scale, scale, cx, cy)
                }
                if (rotation != 0f) {
                    canvas.rotate(rotation, cx, cy)
                }

                val dp = resources.displayMetrics.density
                // Match celebration glow halo
                if (isMatching && matchGlowAlpha > 0) {
                    matchGlowPaint.alpha = matchGlowAlpha
                    val glowBounds = RectF(slotBounds.left - 6f * dp, slotBounds.top - 6f * dp, slotBounds.right + 6f * dp, slotBounds.bottom + 6f * dp)
                    canvas.drawRoundRect(glowBounds, slotCorner + 6f * dp, slotCorner + 6f * dp, matchGlowPaint)
                }

                // Pair connection glow aura
                if (isPair && pairGlowAlpha > 0) {
                    pairGlowPaint.alpha = pairGlowAlpha
                    val pairBounds = RectF(slotBounds.left - 3f * dp, slotBounds.top - 3f * dp, slotBounds.right + 3f * dp, slotBounds.bottom + 3f * dp)
                    canvas.drawRoundRect(pairBounds, slotCorner + 3f * dp, slotCorner + 3f * dp, pairGlowPaint)
                }

                // Draw Tile Base Bevel
                canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, tileBasePaint)

                // Draw Tile Surface
                faceBounds.set(slotBounds.left, slotBounds.top, slotBounds.right, slotBounds.bottom - 6f)
                canvas.drawRoundRect(faceBounds, slotCorner, slotCorner, tileFacePaint)
                canvas.drawRoundRect(faceBounds, slotCorner, slotCorner, tileStrokePaint)

                // Draw Tile Item Emoji (Sortora Style)
                val emoji = ItemSkinManager.getEmojiForType(context, tile.type)
                PieceRenderer.drawTileItem(canvas, emoji, faceBounds, itemTextPaint, itemShadowPaint)

                // Luminous white radiant energy flash on match
                if (isMatching && matchWhiteFlashAlpha > 0) {
                    whiteFlashPaint.alpha = matchWhiteFlashAlpha
                    canvas.drawRoundRect(faceBounds, slotCorner, slotCorner, whiteFlashPaint)
                }

                canvas.restore()
            }
        }
    }
}
