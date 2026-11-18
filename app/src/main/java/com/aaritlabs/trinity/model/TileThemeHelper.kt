package com.aaritlabs.trinity.model

import android.graphics.Color

object TileThemeHelper {

    /**
     * Returns the signature primary glow color for a given TileType.
     * e.g., Rose -> Deep Ruby Red (#FF1744).
     */
    fun getPrimaryColor(type: TileType): Int {
        return when (type) {
            TileType.ROSE, TileType.STRAWBERRY, TileType.APPLE, TileType.CHERRY, TileType.HEART ->
                Color.parseColor("#FF1744") // Vivid Ruby Red / Crimson

            TileType.BANANA, TileType.PINEAPPLE, TileType.LEMON, TileType.SUNFLOWER, TileType.STAR ->
                Color.parseColor("#FFD600") // Warm Gold

            TileType.ORANGE ->
                Color.parseColor("#FF6D00") // Tangerine Amber

            TileType.KIWI, TileType.AVOCADO, TileType.CLOVER, TileType.LEAF ->
                Color.parseColor("#00E676") // Emerald Mint

            TileType.GRAPE, TileType.BLUEBERRY ->
                Color.parseColor("#9C27B0") // Amethyst Purple

            TileType.WATERMELON ->
                Color.parseColor("#FF5252") // Coral Red

            TileType.PEAR ->
                Color.parseColor("#8BC34A") // Lime Green
        }
    }

    /**
     * Returns a multi-tone particle color palette tailored to each tile's aesthetic:
     * Contains deep primary tones, lighter sparkle tints, and diamond white stars.
     */
    fun getParticlePalette(type: TileType): IntArray {
        return when (type) {
            TileType.ROSE -> intArrayOf(
                Color.parseColor("#FF1744"), // Ruby Red
                Color.parseColor("#FF4081"), // Rose Pink
                Color.parseColor("#FFD700"), // Warm Gold
                Color.parseColor("#40C4FF"), // Sky Blue
                Color.parseColor("#FF80AB"), // Rose Gold
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.STRAWBERRY, TileType.APPLE, TileType.CHERRY, TileType.HEART -> intArrayOf(
                Color.parseColor("#FF1744"), // Ruby Red
                Color.parseColor("#FF5252"), // Crimson
                Color.parseColor("#FF4081"), // Pink
                Color.parseColor("#FFD700"), // Gold accent
                Color.parseColor("#40C4FF"), // Sky Blue
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.BANANA, TileType.PINEAPPLE, TileType.LEMON, TileType.SUNFLOWER, TileType.STAR -> intArrayOf(
                Color.parseColor("#FFD600"), // Gold
                Color.parseColor("#FFAB00"), // Amber
                Color.parseColor("#FF4081"), // Pink glint
                Color.parseColor("#40C4FF"), // Cyan glint
                Color.parseColor("#FFF59D"), // Light Yellow
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.ORANGE -> intArrayOf(
                Color.parseColor("#FF6D00"), // Deep Orange
                Color.parseColor("#FF9100"), // Tangerine
                Color.parseColor("#FFD600"), // Gold
                Color.parseColor("#FF4081"), // Pink sparkle
                Color.parseColor("#40C4FF"), // Sky Cyan
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.KIWI, TileType.AVOCADO, TileType.CLOVER, TileType.LEAF -> intArrayOf(
                Color.parseColor("#00E676"), // Lush Emerald
                Color.parseColor("#69F0AE"), // Mint Sparkle
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#40C4FF"), // Cyan
                Color.parseColor("#FF4081"), // Pink
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.GRAPE, TileType.BLUEBERRY -> intArrayOf(
                Color.parseColor("#7C4DFF"), // Neon Violet
                Color.parseColor("#BA68C8"), // Lavender
                Color.parseColor("#FF4081"), // Pink
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#40C4FF"), // Cyan
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.WATERMELON -> intArrayOf(
                Color.parseColor("#FF5252"), // Watermelon Red
                Color.parseColor("#00E676"), // Lime Green
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#FF80AB"), // Pink Coral
                Color.parseColor("#40C4FF"), // Cyan
                Color.parseColor("#FFFFFF")  // Pure White
            )
            TileType.PEAR -> intArrayOf(
                Color.parseColor("#8BC34A"), // Pear Green
                Color.parseColor("#CDDC39"), // Lime
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#FF4081"), // Pink
                Color.parseColor("#40C4FF"), // Cyan
                Color.parseColor("#FFFFFF")  // Pure White
            )
        }
    }
}
