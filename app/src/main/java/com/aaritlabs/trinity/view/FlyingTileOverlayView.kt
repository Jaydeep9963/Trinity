package com.aaritlabs.trinity.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
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

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Int,
        val color: Int
    )

    private val flyingTiles = CopyOnWriteArrayList<FlyingTile>()
    private val particles = CopyOnWriteArrayList<Particle>()

    private val tileBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BCAAA4") }
    private val tileFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFFF") }
    private val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#D7CCC8")
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val itemShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val currentRect = RectF()
    private val faceRect = RectF()

    init {
        // Transparent overlay
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

    fun burstSparkles(cx: Float, cy: Float) {
        val colors = intArrayOf(
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#FFAB00"), // Amber
            Color.parseColor("#FFFFFF"), // Sparkle white
            Color.parseColor("#00E676"), // Emerald
            Color.parseColor("#00E5FF")  // Cyan
        )
        val rng = Random.Default
        val newParticles = mutableListOf<Particle>()

        for (i in 0 until 18) {
            val angle = (i * (360f / 18) + rng.nextFloat() * 20f) * (Math.PI / 180f)
            val speed = 8f + rng.nextFloat() * 14f
            newParticles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat() - 4f,
                    size = 6f + rng.nextFloat() * 8f,
                    alpha = 255,
                    color = colors[rng.nextInt(colors.size)]
                )
            )
        }
        particles.addAll(newParticles)

        val particleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380
            addUpdateListener {
                for (p in newParticles) {
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.6f // Gravity
                    p.vx *= 0.94f // Air resistance
                    p.alpha = ((1f - it.animatedFraction) * 255).toInt().coerceIn(0, 255)
                    p.size *= 0.96f
                }
                invalidate()
            }
        }
        particleAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                particles.removeAll(newParticles.toSet())
                invalidate()
            }
        })
        particleAnim.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw flying tiles
        for (ft in flyingTiles) {
            val t = ft.progress
            // Quadratic Bezier arc: arch slightly outward
            val arcControlX = (ft.startRect.centerX() + ft.destRect.centerX()) / 2f + (ft.destRect.centerX() - ft.startRect.centerX()) * 0.15f
            val arcControlY = minOf(ft.startRect.centerY(), ft.destRect.centerY()) - 40f

            val curX = (1 - t) * (1 - t) * ft.startRect.left + 2 * (1 - t) * t * (arcControlX - ft.destRect.width() / 2f) + t * t * ft.destRect.left
            val curY = (1 - t) * (1 - t) * ft.startRect.top + 2 * (1 - t) * t * (arcControlY - ft.destRect.height() / 2f) + t * t * ft.destRect.top

            val curW = (1 - t) * ft.startRect.width() + t * ft.destRect.width()
            val curH = (1 - t) * ft.startRect.height() + t * ft.destRect.height()

            currentRect.set(curX, curY, curX + curW, curY + curH)
            val corner = 14f

            // Shadow
            canvas.drawRoundRect(currentRect.left + 4f, currentRect.top + 8f, currentRect.right + 4f, currentRect.bottom + 8f, corner, corner, tileBasePaint)
            // Base Bevel
            canvas.drawRoundRect(currentRect, corner, corner, tileBasePaint)
            // Face
            faceRect.set(currentRect.left, currentRect.top, currentRect.right, currentRect.bottom - 6f)
            canvas.drawRoundRect(faceRect, corner, corner, tileFacePaint)
            canvas.drawRoundRect(faceRect, corner, corner, tileStrokePaint)

            // Draw Tile Item Emoji (Sortora Style)
            val emoji = ItemSkinManager.getEmojiForType(context, ft.tile.type)
            PieceRenderer.drawTileItem(canvas, emoji, faceRect, itemTextPaint, itemShadowPaint)
        }

        // Draw sparkle particles
        for (p in particles) {
            particlePaint.color = p.color
            particlePaint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.size / 2f, particlePaint)
        }
    }
}
