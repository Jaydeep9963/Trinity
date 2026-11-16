package com.aaritlabs.trinity.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object PieceRenderer {

    fun drawTileItem(
        canvas: Canvas,
        emoji: String,
        faceBounds: RectF,
        textPaint: Paint,
        shadowPaint: Paint
    ) {
        if (emoji.isEmpty()) return

        val cx = faceBounds.centerX()
        val cy = faceBounds.centerY()
        val radius = minOf(faceBounds.width(), faceBounds.height()) / 2f

        // Soft ambient contact shadow under piece for realistic 3D resting look (Sortora style)
        shadowPaint.style = Paint.Style.FILL
        shadowPaint.color = Color.BLACK
        shadowPaint.alpha = 36
        canvas.drawOval(
            cx - radius * 0.52f,
            cy + radius * 0.40f,
            cx + radius * 0.52f,
            cy + radius * 0.70f,
            shadowPaint
        )

        // Draw color emoji centered using Paint FontMetrics
        val prevSize = textPaint.textSize
        val prevAlign = textPaint.textAlign

        textPaint.textSize = radius * 1.48f
        textPaint.textAlign = Paint.Align.CENTER
        val fontMetrics = textPaint.fontMetrics
        val textY = cy - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(emoji, cx, textY, textPaint)

        textPaint.textSize = prevSize
        textPaint.textAlign = prevAlign
    }
}
