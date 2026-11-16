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
        color = Color.parseColor("#80FFD700")
    }

    private val trayBounds = RectF()
    private val slotBounds = RectF()
    private val faceBounds = RectF()

    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Flash/Match animation
    private var matchingIndices: Set<Int> = emptySet()
    private var matchAnimScale: Float = 1.0f

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

    fun animateMatch(indices: Set<Int>, onEnd: () -> Unit) {
        matchingIndices = indices
        val anim = ValueAnimator.ofFloat(1.0f, 1.35f, 0.0f).apply {
            duration = 260
            interpolator = OvershootInterpolator()
            addUpdateListener {
                matchAnimScale = it.animatedValue as Float
                invalidate()
            }
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                matchingIndices = emptySet()
                matchAnimScale = 1.0f
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
        for (i in 0 until maxSlots) {
            val sx = startX + i * (slotW + slotSpacing)
            slotBounds.set(sx, startY, sx + slotW, startY + slotH)
            canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, slotEmptyPaint)
            canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, slotStrokePaint)

            // If this slot is occupied by a tile
            if (i < tiles.size) {
                val tile = tiles[i]
                val isMatching = matchingIndices.contains(i)
                val isNewlyInserted = (i == newlyInsertedIndex)

                val scale = when {
                    isMatching -> matchAnimScale
                    isNewlyInserted -> insertBounceScale
                    else -> 1.0f
                }

                canvas.save()
                if (scale != 1.0f) {
                    val cx = slotBounds.centerX()
                    val cy = slotBounds.centerY()
                    canvas.scale(scale, scale, cx, cy)
                }

                // Match glow halo
                if (isMatching) {
                    canvas.drawRoundRect(slotBounds, slotCorner, slotCorner, matchGlowPaint)
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

                canvas.restore()
            }
        }
    }
}
