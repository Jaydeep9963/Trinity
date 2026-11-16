package com.aaritlabs.trinity.model

data class LevelConfig(
    val levelNumber: Int,
    val name: String,
    val tiles: List<Tile>,
    val targetScore: Int = 1000
) {
    val totalTilesCount: Int get() = tiles.size
}
