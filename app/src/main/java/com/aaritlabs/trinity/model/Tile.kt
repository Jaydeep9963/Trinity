package com.aaritlabs.trinity.model

import kotlin.math.abs

data class Tile(
    val id: Int,
    var type: TileType,
    val layer: Int,
    val col: Float,
    val row: Float,
    var isBlocked: Boolean = false,
    var isCollected: Boolean = false
) {
    /**
     * Determines whether two tiles overlap in 2D space.
     * In TileBoardView:
     * - Tile width = 1.0 grid col unit
     * - Row spacing = 1.15 grid cell size, Tile height = 1.18 grid cell size
     *   (tile height in row units is 1.18 / 1.15 = ~1.026)
     * Tiles visually overlap if horizontal distance dx < 0.98f and vertical distance dy < 1.01f.
     * Adjacent grid cells have spacing >= 1.10f, so they will never falsely trigger.
     */
    fun overlaps(other: Tile): Boolean {
        val dx = abs(this.col - other.col)
        val dy = abs(this.row - other.row)
        return dx < 0.92f && dy < 0.92f
    }

    fun copyTile(): Tile {
        return Tile(
            id = id,
            type = type,
            layer = layer,
            col = col,
            row = row,
            isBlocked = isBlocked,
            isCollected = isCollected
        )
    }
}
