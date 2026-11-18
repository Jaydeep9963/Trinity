package com.aaritlabs.trinity.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import com.aaritlabs.trinity.data.ItemSkinManager
import com.aaritlabs.trinity.model.Tile
import com.aaritlabs.trinity.model.TileType
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FlyingTileOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class FlyingTile(
        val tile: Tile,
        val startRect: RectF,
        val destRect: RectF,
        var progress: Float = 0f,
        val onComplete: () -> Unit
    )

    private data class SparkleStar(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val baseRadius: Float,
        var alpha: Int,
        val color: Int,
        var rotation: Float,
        val rotSpeed: Float,
        var scale: Float = 0.2f
    )

    private data class FloatingText(
        val text: String,
        var x: Float,
        var y: Float,
        var vy: Float,
        var scale: Float,
        var alpha: Int,
        val color: Int
    )

    private val flyingTiles = CopyOnWriteArrayList<FlyingTile>()
    private val stars = CopyOnWriteArrayList<SparkleStar>()
    private val floatingTexts = CopyOnWriteArrayList<FloatingText>()

    private val tileBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BCAAA4") }
    private val tileFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFFF") }
    private val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#D7CCC8")
    }

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val coreWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val floatingTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val floatingTextStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
    }

    private val starPath = Path()
    private val innerStarPath = Path()
    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val currentRect = RectF()
    private val faceRect = RectF()

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun flyTile(tile: Tile, startRect: RectF, destRect: RectF, onComplete: () -> Unit) {
        val ft = FlyingTile(tile, RectF(startRect), RectF(destRect), 0f, onComplete)
        flyingTiles.add(ft)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 230
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                ft.progress = it.animatedValue as Float
                invalidate()
            }
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                flyingTiles.remove(ft)
                invalidate()
                ft.onComplete()
            }
        })
        anim.start()
    }

    /**
     * Builds the exact 4-point concave diamond astroid star (✦) matching the reference screenshot.
     */
    private fun buildSparkleStarPath(path: Path, cx: Float, cy: Float, radius: Float, rotationDeg: Float) {
        path.reset()
        val rad = Math.toRadians(rotationDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        fun rotX(x: Float, y: Float): Float = cx + x * cosA - y * sinA
        fun rotY(x: Float, y: Float): Float = cy + x * sinA + y * cosA

        val r = radius
        val pinch = r * 0.18f // Deep elegant pinch toward the center

        path.moveTo(rotX(0f, -r), rotY(0f, -r))
        path.quadTo(rotX(pinch, -pinch), rotY(pinch, -pinch), rotX(r, 0f), rotY(r, 0f))
        path.quadTo(rotX(pinch, pinch), rotY(pinch, pinch), rotX(0f, r), rotY(0f, r))
        path.quadTo(rotX(-pinch, pinch), rotY(-pinch, pinch), rotX(-r, 0f), rotY(-r, 0f))
        path.quadTo(rotX(-pinch, -pinch), rotY(-pinch, -pinch), rotX(0f, -r), rotY(0f, -r))
        path.close()
    }

    /**
     * Spectacular Pair Clash:
     * When 2 tiles meet, a fountain of pure 4-point sparkle stars (✦) shoots upward from the seam.
     */
    fun burstPairClash(cx: Float, cy: Float, type: TileType) {
        val dp = resources.displayMetrics.density
        val palette = com.aaritlabs.trinity.model.TileThemeHelper.getParticlePalette(type)
        val rng = Random.Default

        val newStars = mutableListOf<SparkleStar>()
        // 20 sparkle stars in various sizes (small, medium, large)
        for (i in 0 until 20) {
            val angle = (-90f + (rng.nextFloat() - 0.5f) * 110f) * (Math.PI / 180f)
            val speed = (3.5f + rng.nextFloat() * 10f) * dp
            val baseRadius = (7f + rng.nextFloat() * 14f) * dp
            newStars.add(
                SparkleStar(
                    x = cx + (rng.nextFloat() - 0.5f) * 12f * dp,
                    y = cy + (rng.nextFloat() - 0.5f) * 8f * dp,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat() - 2.5f * dp,
                    baseRadius = baseRadius,
                    alpha = 255,
                    color = palette[rng.nextInt(palette.size)],
                    rotation = rng.nextFloat() * 90f,
                    rotSpeed = (rng.nextFloat() - 0.5f) * 16f,
                    scale = 0.2f
                )
            )
        }
        stars.addAll(newStars)

        val starAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 460
            addUpdateListener { va ->
                val f = va.animatedFraction
                for (s in newStars) {
                    s.x += s.vx
                    s.y += s.vy
                    s.vy += 0.35f * dp // Gentle gravity
                    s.vx *= 0.94f
                    s.rotation += s.rotSpeed
                    // Scale pop: 0.2 -> 1.25 -> 0.8
                    s.scale = if (f < 0.25f) (0.2f + (f / 0.25f) * 1.05f) else (1.25f - ((f - 0.25f) / 0.75f) * 0.45f)
                    // Alpha fadeout
                    s.alpha = if (f < 0.5f) 255 else ((1f - (f - 0.5f) / 0.5f) * 255).toInt().coerceIn(0, 255)
                }
                invalidate()
            }
        }
        starAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                stars.removeAll(newStars.toSet())
                invalidate()
            }
        })
        starAnim.start()
    }

    /**
     * Dense, Magical Match-3 Starburst exactly like the reference screenshot:
     * - A rich cloud of 52 twinkling 4-point sparkle stars in multi-colors with glowing white centers
     * - Varied star sizes (large, medium, tiny glints)
     * - Floating score banner (+100 / COMBO!)
     */
    fun burstMatchClash(cx: Float, cy: Float, type: TileType, combo: Int = 1, points: Int = 100) {
        val dp = resources.displayMetrics.density
        val palette = com.aaritlabs.trinity.model.TileThemeHelper.getParticlePalette(type)
        val rng = Random.Default

        val newStars = mutableListOf<SparkleStar>()
        // 52 sparkling stars scattering in a full dense cloud
        for (i in 0 until 52) {
            val angle = (i * (360f / 52) + rng.nextFloat() * 18f) * (Math.PI / 180f)
            val speed = (4f + rng.nextFloat() * 15f) * dp
            // Mixture of large, medium, and small stars
            val baseRadius = when (i % 5) {
                0 -> (18f + rng.nextFloat() * 8f) * dp // Extra large feature stars
                1, 2 -> (12f + rng.nextFloat() * 6f) * dp // Medium stars
                else -> (6f + rng.nextFloat() * 5f) * dp // Small twinkle glints
            }
            newStars.add(
                SparkleStar(
                    x = cx + (rng.nextFloat() - 0.5f) * 24f * dp,
                    y = cy + (rng.nextFloat() - 0.5f) * 16f * dp,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat() - 3.5f * dp,
                    baseRadius = baseRadius,
                    alpha = 255,
                    color = palette[rng.nextInt(palette.size)],
                    rotation = rng.nextFloat() * 90f,
                    rotSpeed = (rng.nextFloat() - 0.5f) * 20f,
                    scale = 0.2f
                )
            )
        }
        stars.addAll(newStars)

        val starAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 580
            addUpdateListener { va ->
                val f = va.animatedFraction
                for (s in newStars) {
                    s.x += s.vx
                    s.y += s.vy
                    s.vy += 0.38f * dp
                    s.vx *= 0.94f
                    s.rotation += s.rotSpeed
                    // Dynamic twinkling scale pop
                    s.scale = if (f < 0.25f) (0.2f + (f / 0.25f) * 1.05f) else (1.25f - ((f - 0.25f) / 0.75f) * 0.45f)
                    // Smooth alpha fade
                    s.alpha = if (f < 0.55f) 255 else ((1f - (f - 0.55f) / 0.45f) * 255).toInt().coerceIn(0, 255)
                }
                invalidate()
            }
        }
        starAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                stars.removeAll(newStars.toSet())
                invalidate()
            }
        })
        starAnim.start()

        // Floating Celebratory Score Banner (+100 / COMBO!)
        val label = if (combo > 1) "COMBO x$combo! +$points" else "MATCH! +$points"
        val primaryColor = com.aaritlabs.trinity.model.TileThemeHelper.getPrimaryColor(type)
        val textColor = if (combo > 1) Color.parseColor("#FFD700") else primaryColor
        val ft = FloatingText(
            text = label,
            x = cx,
            y = cy - 14f * dp,
            vy = -2.0f * dp,
            scale = 0.4f,
            alpha = 255,
            color = textColor
        )
        floatingTexts.add(ft)

        val textAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 620
            addUpdateListener {
                val f = it.animatedValue as Float
                ft.y += ft.vy
                ft.vy *= 0.93f
                ft.scale = if (f < 0.35f) (0.4f + (f / 0.35f) * 0.90f) else (1.30f - ((f - 0.35f) / 0.65f) * 0.30f)
                ft.alpha = if (f < 0.60f) 255 else ((1f - (f - 0.60f) / 0.40f) * 255).toInt().coerceIn(0, 255)
                invalidate()
            }
        }
        textAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                floatingTexts.remove(ft)
                invalidate()
            }
        })
        textAnim.start()
    }

    /**
     * Backward-compatible fallbacks using the exact 4-point sparkle stars
     */
    fun burstPairSparkles(cx: Float, cy: Float) {
        burstPairClash(cx, cy, TileType.ROSE)
    }

    fun burstSparkles(cx: Float, cy: Float) {
        burstMatchClash(cx, cy, TileType.ROSE, 1, 100)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dp = resources.displayMetrics.density

        // 1. Draw Flying Tiles
        for (ft in flyingTiles) {
            val t = ft.progress
            val arcControlX = (ft.startRect.centerX() + ft.destRect.centerX()) / 2f + (ft.destRect.centerX() - ft.startRect.centerX()) * 0.15f
            val arcControlY = minOf(ft.startRect.centerY(), ft.destRect.centerY()) - 40f

            val curX = (1 - t) * (1 - t) * ft.startRect.left + 2 * (1 - t) * t * (arcControlX - ft.destRect.width() / 2f) + t * t * ft.destRect.left
            val curY = (1 - t) * (1 - t) * ft.startRect.top + 2 * (1 - t) * t * (arcControlY - ft.destRect.height() / 2f) + t * t * ft.destRect.top

            val curW = (1 - t) * ft.startRect.width() + t * ft.destRect.width()
            val curH = (1 - t) * ft.startRect.height() + t * ft.destRect.height()

            currentRect.set(curX, curY, curX + curW, curY + curH)
            val corner = 14f

            canvas.drawRoundRect(currentRect.left + 4f, currentRect.top + 8f, currentRect.right + 4f, currentRect.bottom + 8f, corner, corner, tileBasePaint)
            canvas.drawRoundRect(currentRect, corner, corner, tileBasePaint)
            faceRect.set(currentRect.left, currentRect.top, currentRect.right, currentRect.bottom - 6f)
            canvas.drawRoundRect(faceRect, corner, corner, tileFacePaint)
            canvas.drawRoundRect(faceRect, corner, corner, tileStrokePaint)

            val emoji = ItemSkinManager.getEmojiForType(context, ft.tile.type)
            PieceRenderer.drawTileItem(canvas, emoji, faceRect, itemTextPaint, itemShadowPaint)
        }

        // 2. Draw 4-point Sparkle Stars (Outer colored star + glowing white inner core)
        for (s in stars) {
            if (s.alpha <= 0) continue
            val r = s.baseRadius * s.scale
            if (r <= 0.5f) continue

            // Outer colored star
            starPaint.color = s.color
            starPaint.alpha = s.alpha
            buildSparkleStarPath(starPath, s.x, s.y, r, s.rotation)
            canvas.drawPath(starPath, starPaint)

            // Inner glowing white core
            val innerR = r * 0.45f
            if (innerR > 1f) {
                coreWhitePaint.alpha = (s.alpha * 0.92f).toInt()
                buildSparkleStarPath(innerStarPath, s.x, s.y, innerR, s.rotation)
                canvas.drawPath(innerStarPath, coreWhitePaint)
            }
        }

        // 3. Draw Floating Celebratory Text Banners (+100 / COMBO!)
        for (ft in floatingTexts) {
            canvas.save()
            canvas.scale(ft.scale, ft.scale, ft.x, ft.y)
            floatingTextStrokePaint.strokeWidth = 4.5f * dp
            floatingTextStrokePaint.color = Color.parseColor("#44000000")
            floatingTextStrokePaint.textSize = 21f * dp
            floatingTextStrokePaint.alpha = ft.alpha
            canvas.drawText(ft.text, ft.x, ft.y, floatingTextStrokePaint)

            floatingTextPaint.textSize = 21f * dp
            floatingTextPaint.color = ft.color
            floatingTextPaint.alpha = ft.alpha
            canvas.drawText(ft.text, ft.x, ft.y, floatingTextPaint)
            canvas.restore()
        }
    }
}
