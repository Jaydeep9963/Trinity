package com.aaritlabs.trinity.engine

import com.aaritlabs.trinity.model.LevelConfig
import com.aaritlabs.trinity.model.Tile
import com.aaritlabs.trinity.model.TileType
import java.util.Random

object LevelGenerator {

    fun generateLevel(levelNum: Int): LevelConfig {
        return when (levelNum) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            4 -> createLevel4()
            5 -> createLevel5()
            6 -> createLevel6()
            7 -> createLevel7()
            8 -> createLevel8()
            else -> createProceduralLevel(levelNum)
        }
    }

    private fun createLevel1(): LevelConfig {
        // 9 tiles (3 trios) - 2 layers: 2x3 base + 3 on top
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        // Layer 0: 6 tiles
        for (r in 0..1) {
            for (c in 0..2) {
                positions.add(Triple(0, c * 1.2f + 1.8f, r * 1.2f + 2.0f))
            }
        }
        // Layer 1: 3 tiles overlapping
        for (c in 0..2) {
            positions.add(Triple(1, c * 1.2f + 1.8f, 2.6f))
        }

        val tiles = assignTileTypes(positions, level = 1, seed = 101)
        return LevelConfig(1, "Garden Welcome", tiles, 1000)
    }

    private fun createLevel2(): LevelConfig {
        // 18 tiles (6 trios) - 3 layers: Pyramid peak
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        // Layer 0: 3x3 = 9 tiles
        for (r in 0..2) {
            for (c in 0..2) {
                positions.add(Triple(0, c * 1.2f + 1.8f, r * 1.2f + 1.5f))
            }
        }
        // Layer 1: 2x3 = 6 tiles
        for (r in 0..1) {
            for (c in 0..2) {
                positions.add(Triple(1, c * 1.2f + 1.8f, r * 1.2f + 2.1f))
            }
        }
        // Layer 2: 3 tiles
        for (c in 0..2) {
            positions.add(Triple(2, c * 1.2f + 1.8f, 2.7f))
        }

        val tiles = assignTileTypes(positions, level = 2, seed = 202)
        return LevelConfig(2, "Step Pyramid", tiles, 1500)
    }

    private fun createLevel3(): LevelConfig {
        // 27 tiles (9 trios) - Twin Peaks
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        // Left Peak (cols 0.8, 1.9) & Right Peak (cols 4.1, 5.2)
        // Layer 0: 2 blocks of 2x3 = 12 tiles
        for (r in 0..2) {
            for (c in 0..1) {
                positions.add(Triple(0, c * 1.1f + 0.8f, r * 1.1f + 1.5f))
                positions.add(Triple(0, c * 1.1f + 4.1f, r * 1.1f + 1.5f))
            }
        }
        // Center bridge Layer 0: 3 tiles (col 3.0f, exactly between 1.9 and 4.1)
        for (r in 0..2) {
            positions.add(Triple(0, 3.0f, r * 1.1f + 1.5f))
        }
        // Layer 1: 2 blocks of 2x2 = 8 tiles
        for (r in 0..1) {
            for (c in 0..1) {
                positions.add(Triple(1, c * 1.1f + 1.35f, r * 1.1f + 2.05f))
                positions.add(Triple(1, c * 1.1f + 3.55f, r * 1.1f + 2.05f))
            }
        }
        // Layer 2: 4 peak tiles
        positions.add(Triple(2, 1.9f, 2.6f))
        positions.add(Triple(2, 4.1f, 2.6f))
        positions.add(Triple(2, 3.0f, 1.8f))
        positions.add(Triple(2, 3.0f, 3.2f))

        val tiles = assignTileTypes(positions, level = 3, seed = 303)
        return LevelConfig(3, "Twin Peaks", tiles, 2000)
    }

    private fun createLevel4(): LevelConfig {
        // 36 tiles (12 trios) - Diamond Cross
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        val colStep = 1.15f
        val rowStep = 1.15f

        // Layer 0: 4x4 base (16 tiles) + 4 diamond cross tips = 20 tiles
        for (r in 0..3) {
            for (c in 0..3) {
                positions.add(Triple(0, c * colStep + 1.0f, r * rowStep + 1.2f))
            }
        }
        positions.add(Triple(0, 1.5f * colStep + 1.0f, -1f * rowStep + 1.2f)) // Top
        positions.add(Triple(0, 1.5f * colStep + 1.0f, 4f * rowStep + 1.2f))  // Bottom
        positions.add(Triple(0, -1f * colStep + 1.0f, 1.5f * rowStep + 1.2f)) // Left
        positions.add(Triple(0, 4f * colStep + 1.0f, 1.5f * rowStep + 1.2f))  // Right

        // Layer 1: 3x3 center = 9 tiles (shifted by half-step)
        for (r in 0..2) {
            for (c in 0..2) {
                positions.add(Triple(1, (c + 0.5f) * colStep + 1.0f, (r + 0.5f) * rowStep + 1.2f))
            }
        }

        // Layer 2: 2x2 = 4 tiles (integer grid)
        for (r in 1..2) {
            for (c in 1..2) {
                positions.add(Triple(2, c * colStep + 1.0f, r * rowStep + 1.2f))
            }
        }

        // Layer 3, 4, 5: 3 peak tiles stacked in exact center
        positions.add(Triple(3, 1.5f * colStep + 1.0f, 1.5f * rowStep + 1.2f))
        positions.add(Triple(4, 1.5f * colStep + 1.0f, 1.5f * rowStep + 1.2f))
        positions.add(Triple(5, 1.5f * colStep + 1.0f, 1.5f * rowStep + 1.2f))

        // Total = 20 + 9 + 4 + 3 = 36 tiles (12 trios)
        val tiles = assignTileTypes(positions, level = 4, seed = 404)
        return LevelConfig(4, "Diamond Cross", tiles, 2500)
    }

    private fun createLevel5(): LevelConfig {
        // 45 tiles (15 trios) - Turtle Shell
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        // Layer 0: 5x5 hexagon-like = 21 tiles
        for (r in 0..4) {
            for (c in 0..4) {
                if (!((r == 0 && c == 0) || (r == 0 && c == 4) || (r == 4 && c == 0) || (r == 4 && c == 4))) {
                    positions.add(Triple(0, c * 1.1f + 0.8f, r * 1.1f + 1.0f))
                }
            }
        }
        // Layer 1: 4x4 inner = 14 tiles
        for (r in 0..3) {
            for (c in 0..3) {
                if (!((r == 0 && c == 0) || (r == 3 && c == 3))) {
                    positions.add(Triple(1, c * 1.1f + 1.35f, r * 1.1f + 1.55f))
                }
            }
        }
        // Layer 2: 7 tiles
        for (r in 0..2) {
            for (c in 0..2) {
                if ((r + c) % 2 == 0 || (r == 1 && c == 1) || (r == 0 && c == 1) || (r == 2 && c == 1)) {
                    positions.add(Triple(2, c * 1.1f + 1.9f, r * 1.1f + 2.1f))
                }
            }
        }
        // Layer 3: Top = 3 tiles
        positions.add(Triple(3, 2.45f, 2.65f))
        positions.add(Triple(3, 3.55f, 2.65f))
        positions.add(Triple(3, 3.0f, 3.75f))

        // Total = 21 + 14 + 7 + 3 = 45 tiles (15 trios)
        val tiles = assignTileTypes(positions, level = 5, seed = 505)
        return LevelConfig(5, "Turtle Shell", tiles, 3000)
    }

    private fun createLevel6(): LevelConfig {
        // 54 tiles (18 trios) - Butterfly Wings
        // Discrete 5-column grid: zero same-layer collisions, clean multi-layer pyramid depth
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        val colStep = 1.15f
        val rowStep = 1.15f
        val x0 = 0.70f
        val y0 = 1.20f

        // Layer 0: Base Wings + Spine (24 tiles)
        for (c in 0..4) {
            for (r in 0..3) {
                positions.add(Triple(0, c * colStep + x0, r * rowStep + y0))
            }
        }
        positions.add(Triple(0, 0f * colStep + x0, -1f * rowStep + y0))
        positions.add(Triple(0, 4f * colStep + x0, -1f * rowStep + y0))
        positions.add(Triple(0, 0f * colStep + x0, 4f * rowStep + y0))
        positions.add(Triple(0, 4f * colStep + x0, 4f * rowStep + y0))

        // Layer 1: Mid Wings (18 tiles, shifted by half-step)
        for (c in 0..3) {
            for (r in 0..2) {
                positions.add(Triple(1, (c + 0.5f) * colStep + x0, (r + 0.5f) * rowStep + y0))
            }
        }
        positions.add(Triple(1, 1.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, 2.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, 1.5f * colStep + x0, 3.5f * rowStep + y0))
        positions.add(Triple(1, 2.5f * colStep + x0, 3.5f * rowStep + y0))
        positions.add(Triple(1, 0.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, 3.5f * colStep + x0, -0.5f * rowStep + y0))

        // Layer 2: Top Wings & Spine (12 tiles, integer grid)
        for (c in 1..3) {
            for (r in 1..2) {
                positions.add(Triple(2, c * colStep + x0, r * rowStep + y0))
            }
        }
        positions.add(Triple(2, 2f * colStep + x0, 0f * rowStep + y0))
        positions.add(Triple(2, 2f * colStep + x0, 3f * rowStep + y0))
        positions.add(Triple(2, 1f * colStep + x0, 0f * rowStep + y0))
        positions.add(Triple(2, 3f * colStep + x0, 0f * rowStep + y0))
        positions.add(Triple(2, 1f * colStep + x0, 3f * rowStep + y0))
        positions.add(Triple(2, 3f * colStep + x0, 3f * rowStep + y0))

        // Total = 24 + 18 + 12 = 54 tiles (18 trios)
        val tiles = assignTileTypes(positions, level = 6, seed = 606)
        return LevelConfig(6, "Butterfly Wings", tiles, 3500)
    }

    private fun createLevel7(): LevelConfig {
        // 63 tiles (21 trios) - Fortress Citadel
        // Clean concentric fortress layout: 5x5 foundation, 4x4 inner citadel, 3x4 central keep, 3x2 watchtower
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        val colStep = 1.15f
        val rowStep = 1.15f
        val x0 = 0.8f
        val y0 = 1.2f

        // Layer 0: Castle Foundation & Courtyard (27 tiles)
        for (r in 0..4) {
            for (c in 0..4) {
                positions.add(Triple(0, c * colStep + x0, r * rowStep + y0))
            }
        }
        positions.add(Triple(0, 2f * colStep + x0, -1f * rowStep + y0))
        positions.add(Triple(0, 2f * colStep + x0, 5f * rowStep + y0))

        // Layer 1: Citadel Walls (18 tiles, half-step grid)
        for (r in 0..3) {
            for (c in 0..3) {
                positions.add(Triple(1, (c + 0.5f) * colStep + x0, (r + 0.5f) * rowStep + y0))
            }
        }
        positions.add(Triple(1, 1.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, 2.5f * colStep + x0, -0.5f * rowStep + y0))

        // Layer 2: Central Keep (12 tiles, integer grid)
        for (r in 1..4) {
            for (c in 1..3) {
                positions.add(Triple(2, c * colStep + x0, r * rowStep + y0))
            }
        }

        // Layer 3: High Watchtower Spire (6 tiles, centered 3x2 on integer grid)
        for (r in 2..3) {
            for (c in 1..3) {
                positions.add(Triple(3, c * colStep + x0, r * rowStep + y0))
            }
        }

        // Total = 27 + 18 + 12 + 6 = 63 tiles (21 trios)
        val tiles = assignTileTypes(positions, level = 7, seed = 707)
        return LevelConfig(7, "Fortress Citadel", tiles, 4000)
    }

    private fun createLevel8(): LevelConfig {
        // 72 tiles (24 trios) - Spiral Galaxy
        val positions = mutableListOf<Triple<Int, Float, Float>>()
        val colStep = 1.15f
        val rowStep = 1.15f
        val x0 = 1.0f
        val y0 = 1.0f

        // Layer 0 (Outer Ring): 24 tiles
        for (r in 0..4) {
            for (c in 0..5) {
                if (r == 0 || r == 4 || c == 0 || c == 5) {
                    positions.add(Triple(0, c * colStep + x0, r * rowStep + y0))
                }
            }
        }
        positions.add(Triple(0, -1f * colStep + x0, 2f * rowStep + y0))
        positions.add(Triple(0, 6f * colStep + x0, 2f * rowStep + y0))
        positions.add(Triple(0, 2f * colStep + x0, -1f * rowStep + y0))
        positions.add(Triple(0, 3f * colStep + x0, -1f * rowStep + y0))
        positions.add(Triple(0, 2f * colStep + x0, 5f * rowStep + y0))
        positions.add(Triple(0, 3f * colStep + x0, 5f * rowStep + y0))

        // Layer 1 (Mid Outer - shifted by 0.5, 0.5): 21 tiles
        for (r in 0..3) {
            for (c in 0..4) {
                if (r == 0 || r == 3 || c == 0 || c == 4 || (r == 1 && c == 2) || (r == 2 && c == 2) || (r == 1 && c == 1)) {
                    positions.add(Triple(1, (c + 0.5f) * colStep + x0, (r + 0.5f) * rowStep + y0))
                }
            }
        }
        positions.add(Triple(1, 0.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, 4.5f * colStep + x0, -0.5f * rowStep + y0))
        positions.add(Triple(1, -0.5f * colStep + x0, 1.5f * rowStep + y0))
        positions.add(Triple(1, 5.5f * colStep + x0, 1.5f * rowStep + y0))

        // Layer 2 (Mid Inner - integer grid): 18 tiles
        for (r in 1..3) {
            for (c in 1..4) {
                positions.add(Triple(2, c * colStep + x0, r * rowStep + y0))
            }
        }
        positions.add(Triple(2, 2f * colStep + x0, 0f * rowStep + y0))
        positions.add(Triple(2, 3f * colStep + x0, 0f * rowStep + y0))
        positions.add(Triple(2, 2f * colStep + x0, 4f * rowStep + y0))
        positions.add(Triple(2, 3f * colStep + x0, 4f * rowStep + y0))
        positions.add(Triple(2, 0f * colStep + x0, 2f * rowStep + y0))
        positions.add(Triple(2, 5f * colStep + x0, 2f * rowStep + y0))

        // Layer 3 (Galactic Core - shifted by 0.5, 0.5): 9 tiles
        for (r in 1..3) {
            for (c in 1..3) {
                positions.add(Triple(3, (c + 0.5f) * colStep + x0, (r + 0.5f) * rowStep + y0))
            }
        }

        // Total = 24 + 21 + 18 + 9 = 72 tiles (24 trios)
        val tiles = assignTileTypes(positions, level = 8, seed = 808)
        return LevelConfig(8, "Spiral Galaxy", tiles, 4500)
    }

    private fun createProceduralLevel(levelNum: Int): LevelConfig {
        // Deterministic generation based on levelNum
        val rand = Random(levelNum.toLong() * 9973L)
        // Number of trios scales with level: 12 to 26 trios (36 to 78 tiles)
        val numTrios = (12 + (levelNum % 15)).coerceIn(12, 26)
        val totalTiles = numTrios * 3
        val maxLayers = 4 + (levelNum % 2)

        val positions = mutableListOf<Triple<Int, Float, Float>>()
        val colStep = 1.15f
        val rowStep = 1.15f
        val centerCol = 2.8f
        val centerRow = 3.0f

        val tilesPerLayer = totalTiles / maxLayers
        val occupiedByLayer = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()

        // Generate layers with pyramid / concentric step
        for (layer in 0 until maxLayers) {
            val layerOccupied = mutableSetOf<Pair<Int, Int>>()
            occupiedByLayer[layer] = layerOccupied
            val isStaggered = (layer % 2 == 1)
            val halfShift = if (isStaggered) 0.5f else 0.0f
            val countForThisLayer = if (layer == maxLayers - 1) totalTiles - positions.size else tilesPerLayer

            // Candidate grid cells expanding outward from center
            val gridRadius = (4 - layer).coerceAtLeast(1)
            val candidateCells = mutableListOf<Pair<Int, Int>>()
            for (r in -gridRadius..gridRadius) {
                for (c in -gridRadius..gridRadius) {
                    candidateCells.add(Pair(c, r))
                }
            }
            candidateCells.shuffle(rand)

            var added = 0
            for (cell in candidateCells) {
                if (added >= countForThisLayer) break
                val (c, r) = cell
                if (!layerOccupied.contains(cell)) {
                    layerOccupied.add(cell)
                    val col = centerCol + (c.toFloat() + halfShift) * colStep
                    val row = centerRow + (r.toFloat() + halfShift) * rowStep
                    positions.add(Triple(layer, col, row))
                    added++
                }
            }
        }

        // Safety: strictly ensure multiple of 3
        while (positions.size % 3 != 0) {
            positions.removeAt(positions.lastIndex)
        }

        val tiles = assignTileTypes(positions, levelNum, rand.nextLong())
        val names = listOf("Emerald Forest", "Sunken Temple", "Golden Oasis", "Crystal Cavern", "Mystic Shrine", "Starry Heights")
        val levelName = names[(levelNum - 1) % names.size] + " " + levelNum
        return LevelConfig(levelNum, levelName, tiles, 1000 + levelNum * 250)
    }

    private fun assignTileTypes(
        positions: List<Triple<Int, Float, Float>>,
        level: Int,
        seed: Long
    ): List<Tile> {
        val adjustedPositions = positions.toMutableList()
        // Universal safety: trio games strictly require total tiles to be divisible by 3
        while (adjustedPositions.size % 3 != 0) {
            adjustedPositions.removeAt(adjustedPositions.lastIndex)
        }
        val totalTiles = adjustedPositions.size
        val numTrios = totalTiles / 3
        val rand = Random(seed)

        // Select how many distinct tile types to use (starts at 5 for early levels, up to 16)
        val availableTypesCount = (5 + level).coerceAtMost(TileType.entries.size)
        val allTypes = TileType.entries.take(availableTypesCount)

        // Choose types for each trio
        val tileTypesList = mutableListOf<TileType>()
        for (i in 0 until numTrios) {
            val chosenType = allTypes[rand.nextInt(allTypes.size)]
            tileTypesList.add(chosenType)
            tileTypesList.add(chosenType)
            tileTypesList.add(chosenType)
        }

        // Shuffle the types across the board positions
        tileTypesList.shuffle(rand)

        val tiles = mutableListOf<Tile>()
        for (i in 0 until totalTiles) {
            val (layer, col, row) = adjustedPositions[i]
            tiles.add(
                Tile(
                    id = i,
                    type = tileTypesList[i],
                    layer = layer,
                    col = col,
                    row = row
                )
            )
        }

        // Recalculate blockage states
        updateBlockages(tiles)
        return tiles
    }

    fun updateBlockages(tiles: List<Tile>) {
        val uncollected = tiles.filter { !it.isCollected }
        for (tile in uncollected) {
            // A tile is blocked (inactive) strictly if an uncollected tile at a higher layer is on top of it.
            // Otherwise, it is active!
            tile.isBlocked = uncollected.any { other ->
                other.id != tile.id && other.layer > tile.layer && tile.overlaps(other)
            }
        }
    }
}
