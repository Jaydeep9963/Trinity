package com.aaritlabs.trinity.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.aaritlabs.trinity.data.ItemSkinManager
import com.aaritlabs.trinity.model.Tile
import com.aaritlabs.trinity.model.TileType

class TileBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tiles: List<Tile> = emptyList()
    var onTileClickListener: ((Tile) -> Unit)? = null
    var onBlockedTileClickListener: ((Tile) -> Unit)? = null

    // Dimensional calculations
    var tileWidth = 0f
        private set
    var tileHeight = 0f
        private set
    private var gridOffsetX = 0f
    private var gridOffsetY = 0f
    private var gridCellSize = 0f
    private val rowStepFactor = 1.16f

    // Drawing paints
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bevelBasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileFacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val blockedOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000")
    }
    private val hintFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#55FFD700")
    }
    private val hintHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#FFFFD700")
    }

    private val tileBounds = RectF()
    private val shadowBounds = RectF()
    private val faceBounds = RectF()

    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val highlightedTileIds = mutableSetOf<Int>()

    // Touch feedback animation
    private var animatingTileId: Int? = null
    private var animatingScale: Float = 1.0f

    // Hint pulse animator
    private var hintGlowAlpha = 255
    private var hintPulseScale = 1.0f
    private var hintAnimator: ValueAnimator? = null

    init {
        updateThemePaints()
        startHintPulseAnimation()
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
            shadowPaint.color = Color.parseColor("#88000000")
            bevelBasePaint.color = Color.parseColor("#475569")
            tileFacePaint.color = Color.parseColor("#FFFFFF")
            tileStrokePaint.color = Color.parseColor("#334155")
        } else {
            shadowPaint.color = Color.parseColor("#26000000")
            bevelBasePaint.color = Color.parseColor("#CBD5E1")
            tileFacePaint.color = Color.parseColor("#FFFFFF")
            tileStrokePaint.color = Color.parseColor("#E2E8F0")
        }
    }

    private fun startHintPulseAnimation() {
        hintAnimator = ValueAnimator.ofFloat(1.0f, 1.10f).apply {
            duration = 450
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                hintPulseScale = it.animatedValue as Float
                hintGlowAlpha = (140 + (hintPulseScale - 1.0f) * 1150).toInt().coerceIn(100, 255)
                if (highlightedTileIds.isNotEmpty()) {
                    hintHighlightPaint.alpha = hintGlowAlpha
                    hintFillPaint.alpha = (hintGlowAlpha * 0.45f).toInt()
                    invalidate()
                }
            }
            start()
        }
    }

    fun setTiles(newTiles: List<Tile>) {
        this.tiles = newTiles
        highlightedTileIds.clear()
        if (width > 0 && height > 0) {
            calculateDimensions(width, height)
        }
        requestLayout()
        invalidate()
    }

    fun highlightTiles(tileIds: Set<Int>) {
        highlightedTileIds.clear()
        highlightedTileIds.addAll(tileIds)
        invalidate()
    }

    fun clearHighlights() {
        if (highlightedTileIds.isNotEmpty()) {
            highlightedTileIds.clear()
            invalidate()
        }
    }

    fun getTilePositionOnScreen(tile: Tile): RectF {
        val x = gridOffsetX + tile.col * gridCellSize
        val y = gridOffsetY + tile.row * gridCellSize * rowStepFactor - (tile.layer * 4f)
        return RectF(x, y, x + tileWidth, y + tileHeight)
    }

    fun animateShuffle(onEnd: () -> Unit) {
        animate()
            .rotationY(90f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(220)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                invalidate()
                rotationY = -90f
                animate()
                    .rotationY(0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(260)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction(onEnd)
                    .start()
            }
            .start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions(w, h)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val w = right - left
        val h = bottom - top
        if (w > 0 && h > 0) {
            calculateDimensions(w, h)
        }
    }

    private fun calculateDimensions(viewW: Int, viewH: Int) {
        if (tiles.isEmpty() || viewW == 0 || viewH == 0) return

        var minCol = Float.MAX_VALUE
        var maxCol = Float.MIN_VALUE
        var minRow = Float.MAX_VALUE
        var maxRow = Float.MIN_VALUE

        for (t in tiles) {
            if (t.col < minCol) minCol = t.col
            if (t.col > maxCol) maxCol = t.col
            if (t.row < minRow) minRow = t.row
            if (t.row > maxRow) maxRow = t.row
        }

        val gridCols = (maxCol - minCol) + 1.15f
        val gridRows = (maxRow - minRow) + 1.15f

        val availableW = viewW * 0.90f
        val availableH = viewH * 0.90f

        val cellW = availableW / gridCols
        val cellH = availableH / (gridRows * rowStepFactor)
        gridCellSize = minOf(cellW, cellH).coerceAtMost(130f).coerceAtLeast(15f)

        tileWidth = gridCellSize * 1.0f
        tileHeight = gridCellSize * 1.12f

        val actualTilesW = (maxCol - minCol) * gridCellSize + tileWidth
        val actualTilesH = (maxRow - minRow) * gridCellSize * rowStepFactor + tileHeight

        gridOffsetX = (viewW - actualTilesW) / 2f - minCol * gridCellSize
        gridOffsetY = (viewH - actualTilesH) / 2f - minRow * gridCellSize * rowStepFactor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (tiles.isEmpty()) return

        updateThemePaints()

        val drawableTiles = tiles.filter { !it.isCollected }
            .sortedWith(compareBy({ it.layer }, { it.row }, { it.col }))

        for (tile in drawableTiles) {
            drawTile(canvas, tile)
        }
    }

    private fun drawTile(canvas: Canvas, tile: Tile) {
        val x = gridOffsetX + tile.col * gridCellSize
        val y = gridOffsetY + tile.row * gridCellSize * rowStepFactor
        val layerElevation = tile.layer * 4f
        val actualY = y - layerElevation

        val radius = 14f
        val bevelHeight = 8f

        val isHinted = highlightedTileIds.contains(tile.id)
        val scale = when {
            tile.id == animatingTileId -> animatingScale
            isHinted -> hintPulseScale
            else -> 1.0f
        }

        canvas.save()
        if (scale != 1.0f) {
            val cx = x + tileWidth / 2f
            val cy = actualY + tileHeight / 2f
            canvas.scale(scale, scale, cx, cy)
        }

        // 1. Draw Drop Shadow
        shadowBounds.set(x + 4f, actualY + 8f, x + tileWidth + 4f, actualY + tileHeight + 8f)
        canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)

        // 2. Draw 3D Bevel Base Slab
        tileBounds.set(x, actualY, x + tileWidth, actualY + tileHeight)
        canvas.drawRoundRect(tileBounds, radius, radius, bevelBasePaint)

        // 3. Draw Tile Surface Face
        faceBounds.set(x, actualY, x + tileWidth, actualY + tileHeight - bevelHeight)
        canvas.drawRoundRect(faceBounds, radius, radius, tileFacePaint)
        canvas.drawRoundRect(faceBounds, radius, radius, tileStrokePaint)

        // 4. Draw Tile Item Emoji (Sortora Style)
        val emoji = ItemSkinManager.getEmojiForType(context, tile.type)
        PieceRenderer.drawTileItem(canvas, emoji, faceBounds, itemTextPaint, itemShadowPaint)

        // 5. Draw Blocked / Covered Shadow Overlay
        if (tile.isBlocked) {
            canvas.drawRoundRect(faceBounds, radius, radius, blockedOverlayPaint)
        }

        // 6. Draw Hint Highlight Glow (Vibrant gold wash + thick neon stroke)
        if (isHinted) {
            canvas.drawRoundRect(faceBounds, radius, radius, hintFillPaint)
            canvas.drawRoundRect(faceBounds, radius, radius, hintHighlightPaint)
        }

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)

        val touchX = event.x
        val touchY = event.y

        val candidateTiles = tiles.filter { !it.isCollected }
            .sortedWith(
                compareByDescending<Tile> { it.layer }
                    .thenByDescending { it.row }
                    .thenByDescending { it.col }
            )

        for (tile in candidateTiles) {
            val x = gridOffsetX + tile.col * gridCellSize
            val y = gridOffsetY + tile.row * gridCellSize * rowStepFactor - (tile.layer * 4f)

            if (touchX >= x && touchX <= x + tileWidth &&
                touchY >= y && touchY <= y + tileHeight
            ) {
                if (!tile.isBlocked) {
                    clearHighlights()
                    animateTileTap(tile)
                    onTileClickListener?.invoke(tile)
                } else {
                    onBlockedTileClickListener?.invoke(tile)
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun animateTileTap(tile: Tile) {
        animatingTileId = tile.id
        val anim = ValueAnimator.ofFloat(1.0f, 1.18f, 1.0f).apply {
            duration = 160
            interpolator = OvershootInterpolator()
            addUpdateListener {
                animatingScale = it.animatedValue as Float
                invalidate()
            }
        }
        anim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hintAnimator?.cancel()
    }
}
