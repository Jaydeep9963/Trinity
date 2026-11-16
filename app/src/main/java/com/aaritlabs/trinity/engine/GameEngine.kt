package com.aaritlabs.trinity.engine

import com.aaritlabs.trinity.model.LevelConfig
import com.aaritlabs.trinity.model.Tile
import com.aaritlabs.trinity.model.TileType
import java.util.Stack

class GameEngine(val levelConfig: LevelConfig) {

    val allTiles: List<Tile> = levelConfig.tiles
    val dockTiles = mutableListOf<Tile>()
    val maxDockCapacity = 7

    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var coinsEarned: Int = 0
        private set

    private val moveHistory = Stack<MoveRecord>()

    data class MoveRecord(
        val tile: Tile,
        val dockInsertIndex: Int
    )

    sealed class TapResult {
        object Ignored : TapResult()
        data class PlacedInDock(val tile: Tile, val dockIndex: Int) : TapResult()
        data class Matched(val matchedType: TileType, val clearedTiles: List<Tile>, val newCombo: Int, val points: Int) : TapResult()
        data class GameOver(val fullDock: List<Tile>) : TapResult()
        data class Victory(val finalScore: Int, val stars: Int, val coins: Int) : TapResult()
    }

    init {
        LevelGenerator.updateBlockages(allTiles)
    }

    val remainingTilesCount: Int
        get() = allTiles.count { !it.isCollected }

    fun canTap(tile: Tile): Boolean {
        if (tile.isCollected || tile.isBlocked) return false
        if (dockTiles.size >= maxDockCapacity) return false
        return true
    }

    fun onTileTapped(tile: Tile): TapResult {
        if (!canTap(tile)) return TapResult.Ignored

        tile.isCollected = true
        LevelGenerator.updateBlockages(allTiles)

        // Find smart insert position in dock (grouping matching tiles together)
        val lastMatchingIndex = dockTiles.indexOfLast { it.type == tile.type }
        val insertIndex = if (lastMatchingIndex != -1) {
            lastMatchingIndex + 1
        } else {
            dockTiles.size
        }

        dockTiles.add(insertIndex, tile)
        moveHistory.push(MoveRecord(tile, insertIndex))

        // Check for match-3
        val matchingGroup = dockTiles.filter { it.type == tile.type }
        if (matchingGroup.size >= 3) {
            val trio = matchingGroup.take(3)
            dockTiles.removeAll(trio)

            combo++
            val points = 100 * combo
            score += points
            coinsEarned += 5 * combo

            // Check if game won
            if (remainingTilesCount == 0 && dockTiles.isEmpty()) {
                val stars = calculateStars()
                return TapResult.Victory(score, stars, coinsEarned)
            }

            return TapResult.Matched(tile.type, trio, combo, points)
        }

        // Check if tray is full (Game Over)
        if (dockTiles.size >= maxDockCapacity) {
            combo = 0
            return TapResult.GameOver(dockTiles.toList())
        }

        return TapResult.PlacedInDock(tile, insertIndex)
    }

    fun undo(): Boolean {
        if (moveHistory.isEmpty() || dockTiles.isEmpty()) return false

        val record = moveHistory.pop()
        val tile = record.tile

        dockTiles.remove(tile)
        tile.isCollected = false
        LevelGenerator.updateBlockages(allTiles)
        return true
    }

    fun shuffle(): Boolean {
        val uncollected = allTiles.filter { !it.isCollected }
        if (uncollected.size < 2) return false

        // Extract types and shuffle
        val types = uncollected.map { it.type }.toMutableList()
        types.shuffle()

        for (i in uncollected.indices) {
            uncollected[i].type = types[i]
        }
        return true
    }

    fun findHintTrio(): List<Tile>? {
        val uncollected = allTiles.filter { !it.isCollected }
        if (uncollected.isEmpty()) return null
        val unblocked = uncollected.filter { !it.isBlocked }

        val dockCounts = dockTiles.groupingBy { it.type }.eachCount()

        // 1. If 2 in dock of same type, find 1 matching unblocked tile on board
        for ((type, count) in dockCounts) {
            if (count == 2) {
                val match = unblocked.firstOrNull { it.type == type }
                if (match != null) {
                    val siblings = uncollected.filter { it.type == type }
                    return siblings.take(3)
                }
            }
        }

        // 2. If 1 in dock of same type, find 2 matching tiles on board (prioritizing unblocked)
        for ((type, count) in dockCounts) {
            if (count == 1) {
                val matches = uncollected.filter { it.type == type }
                if (matches.size >= 2 && matches.any { !it.isBlocked }) {
                    return matches.sortedBy { it.isBlocked }.take(3)
                }
            }
        }

        // 3. Find 3 unblocked tiles of same type on the board
        val unblockedGroups = unblocked.groupBy { it.type }
        for ((_, group) in unblockedGroups) {
            if (group.size >= 3) {
                return group.take(3)
            }
        }

        // 4. Find any trio of same type that has at least 1 unblocked tile right now!
        val allRemainingGroups = uncollected.groupBy { it.type }
        for ((_, group) in allRemainingGroups) {
            if (group.size >= 3 && group.any { !it.isBlocked }) {
                // Sort with unblocked tiles first so player knows which one to tap!
                return group.sortedBy { it.isBlocked }.take(3)
            }
        }

        // 5. Fallback: Any group with at least 3 tiles
        for ((_, group) in allRemainingGroups) {
            if (group.size >= 3) {
                return group.take(3)
            }
        }

        return uncollected.take(3)
    }

    private fun calculateStars(): Int {
        return when {
            score >= levelConfig.targetScore * 1.5 -> 3
            score >= levelConfig.targetScore -> 2
            else -> 1
        }
    }

    fun resetCombo() {
        combo = 0
    }
}
