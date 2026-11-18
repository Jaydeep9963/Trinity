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
    var lastMatchedType: TileType? = null
        private set
    var coinsEarned: Int = 0
        private set

    var startTime: Long = System.currentTimeMillis()
    var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L

    fun onPause() {
        if (pauseStartTime == 0L) {
            pauseStartTime = System.currentTimeMillis()
        }
    }

    fun onResume() {
        if (pauseStartTime > 0L) {
            pausedDuration += (System.currentTimeMillis() - pauseStartTime)
            pauseStartTime = 0L
        }
    }

    val elapsedSeconds: Long
        get() {
            val totalElapsed = System.currentTimeMillis() - startTime - pausedDuration
            return (totalElapsed / 1000).coerceAtLeast(0L)
        }

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
        data class Victory(val finalScore: Int, val stars: Int, val coins: Int, val elapsedSeconds: Long) : TapResult()
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

    fun onTileTapped(tile: Tile, force: Boolean = false): TapResult {
        if (!force && !canTap(tile)) return TapResult.Ignored
        if (tile.isCollected || dockTiles.size >= maxDockCapacity) return TapResult.Ignored

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

            // Consecutive matching of the SAME tile type increases combo!
            if (lastMatchedType == tile.type) {
                combo++
            } else {
                combo = 1
                lastMatchedType = tile.type
            }

            val points = 100 * combo
            val coins = 20 * combo
            score += points
            coinsEarned += coins

            // Check if game won
            if (remainingTilesCount == 0 && dockTiles.isEmpty()) {
                val timeSec = elapsedSeconds
                val stars = calculateStars(timeSec)
                return TapResult.Victory(score, stars, coinsEarned, timeSec)
            }

            return TapResult.Matched(tile.type, trio, combo, points)
        }

        // Check if tray is full (Game Over)
        if (dockTiles.size >= maxDockCapacity) {
            combo = 0
            lastMatchedType = null
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

    /**
     * Finds the board tiles needed to complete or form a 3-match trio.
     * Returns 1 tile (if dock has 2 matching), 2 tiles (if dock has 1 matching),
     * or 3 tiles (if dock has 0 matching).
     */
    fun findHintTrio(): List<Tile>? {
        val uncollected = allTiles.filter { !it.isCollected }
        if (uncollected.isEmpty()) return null
        val unblocked = uncollected.filter { !it.isBlocked }

        val dockCounts = dockTiles.groupingBy { it.type }.eachCount()
        val freeDockSlots = maxDockCapacity - dockTiles.size

        // 1. If 2 in dock of same type and 1 free slot, find 1 matching tile on board
        if (freeDockSlots >= 1) {
            for ((type, count) in dockCounts) {
                if (count == 2) {
                    val match = unblocked.firstOrNull { it.type == type }
                        ?: uncollected.firstOrNull { it.type == type }
                    if (match != null) {
                        return listOf(match)
                    }
                }
            }
        }

        // 2. If 1 in dock of same type and >= 2 free slots, find 2 matching tiles on board
        if (freeDockSlots >= 2) {
            for ((type, count) in dockCounts) {
                if (count == 1) {
                    val matches = uncollected.filter { it.type == type }
                    if (matches.size >= 2) {
                        return matches.sortedBy { it.isBlocked }.take(2)
                    }
                }
            }
        }

        // 3. If >= 3 free slots, find 3 matching tiles on the board (prioritizing completely unblocked)
        if (freeDockSlots >= 3) {
            val unblockedGroups = unblocked.groupBy { it.type }
            for ((_, group) in unblockedGroups) {
                if (group.size >= 3) {
                    return group.take(3)
                }
            }

            // Fallback: 3 matching tiles with least blockages
            val allGroups = uncollected.groupBy { it.type }
            val sortedGroups = allGroups.values
                .filter { it.size >= 3 }
                .sortedByDescending { grp -> grp.count { !it.isBlocked } }

            if (sortedGroups.isNotEmpty()) {
                return sortedGroups.first().sortedBy { it.isBlocked }.take(3)
            }
        }

        // 4. Any available match on board regardless of dock slots (will clear room)
        val allGroups = uncollected.groupBy { it.type }
        for ((_, group) in allGroups) {
            if (group.size >= 3) {
                return group.sortedBy { it.isBlocked }.take(3)
            }
        }

        return uncollected.take(minOf(3, uncollected.size))
    }

    /**
     * Calculates star rating based on completion time:
     * - Under 2 minutes (< 120s): 3 Stars
     * - Under 6 minutes (< 360s): 2 Stars
     * - 6 minutes or more (>= 360s): 1 Star
     */
    fun calculateStars(seconds: Long = elapsedSeconds): Int {
        return when {
            seconds < 120L -> 3
            seconds < 360L -> 2
            else -> 1
        }
    }

    fun resetCombo() {
        combo = 0
        lastMatchedType = null
    }
}
