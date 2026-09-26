package com.example.data

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Procedural level engine that can produce an unlimited number of arrow levels.
 *
 * Rules every generated level follows:
 * - Arrows live on the dot grid and only use straight, 90-degree segments.
 * - Every grid dot belongs to at most one arrow, so arrows never overlap, cross or touch.
 * - Arrow ends never line up end-to-end with another arrow's end, so two arrows can
 *   never be mistaken for one continuous line.
 * - The level is solvable, and at every step at least one arrow can leave.
 *
 * How solvability is guaranteed: arrows are placed in *reverse* removal order. Each new arrow
 * must have a clear exit ray at the moment it is placed, so it will be free to leave once
 * every arrow placed after it has gone. Its body may sit in front of earlier arrows, which is
 * what creates the blocking chains the player has to untangle. Removing an arrow can only
 * free others, never block them, so any sequence of valid taps always reaches the end.
 * Every level is still run through [validate] before it's accepted.
 *
 * Difficulty ramps up through arrow count, arrow length, turns and how deeply arrows block
 * each other. Dot spacing only ever gets slightly tighter as the grid grows, capped at [MAX_GRID].
 */
object LevelGenerator {

    const val MIN_GRID = 5
    const val MAX_GRID = 12

    /** Every straight run between turns is at least this many dot steps long, so paths stay clean. */
    private const val MIN_RUN = 2

    data class Difficulty(
        val cols: Int,
        val rows: Int,
        val targetArrows: Int,
        val minLength: Int,      // in dots, including head and tail
        val maxLength: Int,
        val maxTurns: Int,
        val turnChance: Float,   // chance to turn at each step once the head run is done
        val blockBias: Float,    // how strongly placement prefers blocking existing arrows
        val minDepth: Int        // minimum number of "waves" needed to clear the board
    )

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String,
        val depth: Int = 0,
        val initiallyFree: Int = 0
    )

    private data class Dir(val dx: Int, val dy: Int)

    private val DIRS = listOf(Dir(0, -1), Dir(1, 0), Dir(0, 1), Dir(-1, 0))

    /** Difficulty curve. Smoothly ramps up to level ~90, then keeps varying at the top tier. */
    fun difficultyFor(level: Int): Difficulty {
        val n = level.coerceAtLeast(1)
        val t = ((n - 1) / 90f).coerceIn(0f, 1f)
        val grid = when {
            n <= 3 -> 5
            n <= 6 -> 6
            n <= 10 -> 7
            n <= 16 -> 8
            n <= 25 -> 9
            n <= 40 -> 10
            n <= 65 -> 11
            else -> MAX_GRID
        }
        // Past the curve, alternate between slightly easier and harder levels so it doesn't get stale.
        val wave = if (n > 90) ((n % 7) - 3) / 3f * 0.08f else 0f
        val fill = (0.55f + 0.33f * t + wave).coerceIn(0.5f, 0.9f)
        val maxLength = if (n <= 2) 3 else (5 + (t * 3f).roundToInt()).coerceAtMost(8)
        val minLength = if (n <= 3) 3 else 2
        val avgLength = (minLength + maxLength) / 2f
        // Arrow count never drops from one early level to the next, even when arrows get longer
        val targetArrows = maxOf(((grid * grid * fill) / avgLength).roundToInt(), minOf(n + 2, 8))
        return Difficulty(
            cols = grid,
            rows = grid,
            targetArrows = targetArrows,
            minLength = minLength,
            maxLength = maxLength,
            maxTurns = when {
                n <= 2 -> 0
                n <= 5 -> 1
                n <= 10 -> 2
                else -> (2 + (t * 5f).roundToInt()).coerceAtMost(7)
            },
            turnChance = 0.25f + 0.30f * t,
            blockBias = when {
                n <= 1 -> 0f
                else -> 0.8f + 3.2f * t
            },
            minDepth = when {
                n <= 1 -> 1
                n <= 4 -> 2
                else -> (2 + (t * 5f).roundToInt()).coerceAtMost(7)
            }
        )
    }

    /** Deterministically generates the arrows for [level]. The same level always gives the same layout. */
    fun generate(level: Int, difficulty: Difficulty = difficultyFor(level)): List<ArrowModel> {
        var best: List<ArrowModel>? = null
        var bestScore = Int.MIN_VALUE
        for (attempt in 0 until 40) {
            val rng = Random(level * 7_919L + attempt * 104_729L + 17L)
            val candidate = buildLevel(level, difficulty, rng)
            val result = validate(candidate, difficulty.cols, difficulty.rows)
            if (!result.isValid) continue

            val enoughArrows = candidate.size >= (difficulty.targetArrows * 0.85f).toInt()
            val deepEnough = result.depth >= difficulty.minDepth
            if (enoughArrows && deepEnough) return candidate

            // Keep the closest valid candidate in case no attempt hits every target
            val score = candidate.size * 10 + result.depth * 25
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best ?: fallbackLevel(level, difficulty.cols, difficulty.rows)
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private class Board(val cols: Int, val rows: Int) {
        // owner[col][row] = index of the arrow occupying that dot, or -1
        val owner = Array(cols) { IntArray(rows) { -1 } }
        val arrows = mutableListOf<List<Pair<Int, Int>>>() // cells from tail to head
        val heads = mutableListOf<Dir>()

        fun inside(c: Int, r: Int) = c in 0 until cols && r in 0 until rows
        fun free(c: Int, r: Int) = inside(c, r) && owner[c][r] == -1

        fun rayClear(c: Int, r: Int, d: Dir): Boolean {
            var x = c + d.dx
            var y = r + d.dy
            while (inside(x, y)) {
                if (owner[x][y] != -1) return false
                x += d.dx
                y += d.dy
            }
            return true
        }

        fun place(cells: List<Pair<Int, Int>>, dir: Dir) {
            val idx = arrows.size
            cells.forEach { (c, r) -> owner[c][r] = idx }
            arrows.add(cells)
            heads.add(dir)
        }
    }

    private fun buildLevel(level: Int, d: Difficulty, rng: Random): List<ArrowModel> {
        val board = Board(d.cols, d.rows)
        var failures = 0
        while (board.arrows.size < d.targetArrows && failures < 6) {
            val placed = placeBestArrow(board, d, rng)
            if (placed) failures = 0 else failures++
        }
        return board.arrows.mapIndexed { i, cells -> toArrowModel("g${level}_${i + 1}", cells, board.heads[i]) }
    }

    /** Tries a batch of random candidate arrows and places the one that best fits the difficulty. */
    private fun placeBestArrow(board: Board, d: Difficulty, rng: Random): Boolean {
        // All (head, direction) pairs that currently have a clear way out
        val starts = mutableListOf<Triple<Int, Int, Dir>>()
        for (c in 0 until board.cols) for (r in 0 until board.rows) {
            if (!board.free(c, r)) continue
            for (dir in DIRS) {
                if (board.free(c - dir.dx, r - dir.dy) && board.rayClear(c, r, dir)) {
                    starts.add(Triple(c, r, dir))
                }
            }
        }
        if (starts.isEmpty()) return false

        var bestCells: List<Pair<Int, Int>>? = null
        var bestDir: Dir? = null
        var bestScore = Float.NEGATIVE_INFINITY
        repeat(28) {
            val (hc, hr, dir) = starts[rng.nextInt(starts.size)]
            val cells = growBody(board, hc, hr, dir, d, rng) ?: return@repeat
            if (!endsAreClean(board, cells)) return@repeat

            val blocks = countBlockedByNewArrow(board, cells)
            val turns = countTurns(cells)
            val neighbours = countNeighbours(board, cells)
            val score = blocks * d.blockBias + cells.size * 0.35f + turns * 0.4f * (d.maxTurns > 0).toFloat() +
                neighbours * 0.3f + rng.nextFloat() * 1.2f
            if (score > bestScore) {
                bestScore = score
                bestCells = cells
                bestDir = dir
            }
        }
        val cells = bestCells ?: return false
        board.place(cells, bestDir!!)
        return true
    }

    private fun Boolean.toFloat() = if (this) 1f else 0f

    /**
     * Grows an arrow backwards from its head. Returns cells ordered tail -> head,
     * or null if it couldn't reach the minimum length.
     */
    private fun growBody(board: Board, hc: Int, hr: Int, dir: Dir, d: Difficulty, rng: Random): List<Pair<Int, Int>>? {
        val targetLength = d.minLength + rng.nextInt(d.maxLength - d.minLength + 1)
        // Cells in front of the head: the arrow must never curl back into its own exit path
        val ownRay = mutableSetOf<Pair<Int, Int>>()
        var x = hc + dir.dx
        var y = hr + dir.dy
        while (board.inside(x, y)) {
            ownRay.add(x to y)
            x += dir.dx
            y += dir.dy
        }

        val path = mutableListOf(hc to hr) // head first while growing
        val used = mutableSetOf(hc to hr)
        var heading = Dir(-dir.dx, -dir.dy) // growing away from the head
        var turns = 0
        var runLength = 0
        // Straight run behind the head so the pointer direction reads clearly
        val headRun = if (targetLength >= 4) 2 else 1

        fun canUse(c: Int, r: Int) = board.free(c, r) && (c to r) !in used && (c to r) !in ownRay

        while (path.size < targetLength) {
            val (cc, cr) = path.last()
            val options = mutableListOf<Dir>()
            val straightOk = canUse(cc + heading.dx, cr + heading.dy)
            val mayTurn = runLength >= maxOf(headRun, MIN_RUN) && turns < d.maxTurns && path.size > 1
            val sideDirs = listOf(Dir(heading.dy, -heading.dx), Dir(-heading.dy, heading.dx))
                .filter { canUse(cc + it.dx, cr + it.dy) }

            if (straightOk) options.add(heading)
            val wantTurn = mayTurn && sideDirs.isNotEmpty() && (!straightOk || rng.nextFloat() < d.turnChance)
            val next = when {
                wantTurn -> sideDirs[rng.nextInt(sideDirs.size)]
                straightOk -> heading
                else -> null
            } ?: break

            if (next != heading) {
                turns++
                runLength = 0
                heading = next
            }
            val cell = (cc + next.dx) to (cr + next.dy)
            path.add(cell)
            used.add(cell)
            runLength++
        }

        if (path.size < d.minLength) return null
        // A 1-dot tail after a turn reads as a stray hook; trim it back to the corner
        if (turns > 0 && runLength < MIN_RUN) {
            repeat(runLength) { path.removeAt(path.size - 1) }
            if (path.size < d.minLength) return null
        }
        return path.reversed()
    }

    /**
     * Rejects arrows whose tail lines up end-to-end with another arrow's end on the same axis,
     * which would read as one broken line. The head never needs checking because its exit ray is empty.
     */
    private fun endsAreClean(board: Board, cells: List<Pair<Int, Int>>): Boolean {
        val tail = cells.first()
        val afterTail = cells[1]
        val outX = tail.first - afterTail.first
        val outY = tail.second - afterTail.second
        val nx = tail.first + outX
        val ny = tail.second + outY
        if (!board.inside(nx, ny)) return true
        val otherIdx = board.owner[nx][ny]
        if (otherIdx == -1) return true
        val other = board.arrows[otherIdx]
        val isOtherEnd = (nx to ny) == other.first() || (nx to ny) == other.last()
        if (!isOtherEnd) return true
        // Axis of the other arrow's end segment
        val neighbour = if ((nx to ny) == other.first()) other[1] else other[other.size - 2]
        val otherAxisHorizontal = neighbour.second == ny
        val ourAxisHorizontal = outY == 0
        return otherAxisHorizontal != ourAxisHorizontal
    }

    /** How many already-placed arrows would have the new arrow standing in their exit path. */
    private fun countBlockedByNewArrow(board: Board, cells: List<Pair<Int, Int>>): Int {
        val cellSet = cells.toSet()
        var count = 0
        board.arrows.forEachIndexed { i, arrow ->
            val (c, r) = arrow.last()
            val dir = board.heads[i]
            var x = c + dir.dx
            var y = r + dir.dy
            while (board.inside(x, y)) {
                if ((x to y) in cellSet) {
                    count++
                    break
                }
                x += dir.dx
                y += dir.dy
            }
        }
        return count
    }

    /** Occupied dots next to the new arrow: favours compact, neatly packed layouts over scattered ones. */
    private fun countNeighbours(board: Board, cells: List<Pair<Int, Int>>): Int {
        val own = cells.toSet()
        var count = 0
        for ((c, r) in cells) {
            for (dir in DIRS) {
                val n = (c + dir.dx) to (r + dir.dy)
                if (n !in own && board.inside(n.first, n.second) && board.owner[n.first][n.second] != -1) count++
            }
        }
        return count
    }

    private fun countTurns(cells: List<Pair<Int, Int>>): Int {
        var turns = 0
        for (i in 1 until cells.size - 1) {
            val ax = cells[i].first - cells[i - 1].first
            val ay = cells[i].second - cells[i - 1].second
            val bx = cells[i + 1].first - cells[i].first
            val by = cells[i + 1].second - cells[i].second
            if (ax != bx || ay != by) turns++
        }
        return turns
    }

    /** Converts unit-step cells into the corner-point polyline the game renders. */
    private fun toArrowModel(id: String, cells: List<Pair<Int, Int>>, dir: Dir): ArrowModel {
        val corners = mutableListOf(cells.first())
        for (i in 1 until cells.size - 1) {
            val ax = cells[i].first - cells[i - 1].first
            val ay = cells[i].second - cells[i - 1].second
            val bx = cells[i + 1].first - cells[i].first
            val by = cells[i + 1].second - cells[i].second
            if (ax != bx || ay != by) corners.add(cells[i])
        }
        corners.add(cells.last())
        val direction = when (dir) {
            Dir(0, -1) -> ArrowDirection.UP
            Dir(1, 0) -> ArrowDirection.RIGHT
            Dir(0, 1) -> ArrowDirection.DOWN
            else -> ArrowDirection.LEFT
        }
        return ArrowModel(id, corners.map { GridPoint(it.first, it.second) }, direction)
    }

    /** Simple, always-solvable layout used only if every generation attempt somehow fails. */
    private fun fallbackLevel(level: Int, cols: Int, rows: Int): List<ArrowModel> {
        val arrows = mutableListOf<ArrowModel>()
        var col = 0
        var i = 0
        while (col < cols) {
            val up = i % 2 == 0
            val pts = if (up) listOf(GridPoint(col, rows - 1), GridPoint(col, 0))
            else listOf(GridPoint(col, 0), GridPoint(col, rows - 1))
            arrows.add(ArrowModel("f${level}_${++i}", pts, if (up) ArrowDirection.UP else ArrowDirection.DOWN))
            col += 2
        }
        return arrows
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /** Expands an arrow's corner points into every grid dot it covers, tail to head. */
    fun cellsOf(arrow: ArrowModel): List<GridPoint> {
        val pts = arrow.points
        if (pts.size == 1) return pts
        val out = mutableListOf(pts.first())
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val dx = (b.col - a.col).coerceIn(-1, 1)
            val dy = (b.row - a.row).coerceIn(-1, 1)
            var c = a.col
            var r = a.row
            while (c != b.col || r != b.row) {
                c += dx
                r += dy
                out.add(GridPoint(c, r))
            }
        }
        return out
    }

    /**
     * Checks a level against every layout and fairness rule:
     * straight 90-degree segments, head direction matching the last segment, no shared dots,
     * no end-to-end merging, and a full solve where every step has at least one free arrow.
     */
    fun validate(arrows: List<ArrowModel>, cols: Int, rows: Int): ValidationResult {
        if (arrows.isEmpty()) return ValidationResult(false, "no arrows")
        val owner = HashMap<GridPoint, String>()
        val cellsById = HashMap<String, List<GridPoint>>()

        for (arrow in arrows) {
            val pts = arrow.points
            if (pts.size < 2) return ValidationResult(false, "${arrow.id}: needs at least one segment")
            for (i in 0 until pts.size - 1) {
                val a = pts[i]
                val b = pts[i + 1]
                if (a == b) return ValidationResult(false, "${arrow.id}: zero-length segment")
                if (a.col != b.col && a.row != b.row) return ValidationResult(false, "${arrow.id}: diagonal segment")
            }
            val last = pts[pts.size - 2]
            val head = pts.last()
            val expected = when {
                head.col > last.col -> ArrowDirection.RIGHT
                head.col < last.col -> ArrowDirection.LEFT
                head.row > last.row -> ArrowDirection.DOWN
                else -> ArrowDirection.UP
            }
            if (expected != arrow.direction) return ValidationResult(false, "${arrow.id}: head direction mismatch")

            val cells = cellsOf(arrow)
            if (cells.size != cells.toSet().size) return ValidationResult(false, "${arrow.id}: crosses itself")
            for (cell in cells) {
                if (cell.col !in 0 until cols || cell.row !in 0 until rows) {
                    return ValidationResult(false, "${arrow.id}: outside the grid")
                }
                val prev = owner.put(cell, arrow.id)
                if (prev != null) return ValidationResult(false, "${arrow.id} overlaps $prev at $cell")
            }
            // Must not point back into itself
            var c = head.col + arrow.direction.dx
            var r = head.row + arrow.direction.dy
            while (c in 0 until cols && r in 0 until rows) {
                if (owner[GridPoint(c, r)] == arrow.id || GridPoint(c, r) in cells) {
                    return ValidationResult(false, "${arrow.id}: points into itself")
                }
                c += arrow.direction.dx
                r += arrow.direction.dy
            }
            cellsById[arrow.id] = cells
        }

        // No two arrow ends meeting in a straight line (would look like one merged arrow)
        val ends = HashMap<GridPoint, Pair<String, Boolean>>() // end dot -> (arrow id, end segment is horizontal)
        for ((id, cells) in cellsById) {
            ends[cells.first()] = id to (cells[0].row == cells[1].row)
            ends[cells.last()] = id to (cells[cells.size - 1].row == cells[cells.size - 2].row)
        }
        for ((dot, info) in ends) {
            val (id, horizontal) = info
            val neighbours = if (horizontal) listOf(GridPoint(dot.col - 1, dot.row), GridPoint(dot.col + 1, dot.row))
            else listOf(GridPoint(dot.col, dot.row - 1), GridPoint(dot.col, dot.row + 1))
            for (n in neighbours) {
                val other = ends[n] ?: continue
                if (other.first != id && other.second == horizontal) {
                    return ValidationResult(false, "$id and ${other.first} merge end-to-end at $dot")
                }
            }
        }

        // Solve it: remove every free arrow each round. Removing arrows only frees others,
        // so if this clears the board, any sequence of valid taps does too.
        val remaining = arrows.toMutableList()
        val occupied = HashMap(owner)
        var depth = 0
        var initiallyFree = 0
        while (remaining.isNotEmpty()) {
            val free = remaining.filter { isFree(it, occupied, cols, rows) }
            if (free.isEmpty()) return ValidationResult(false, "deadlock with ${remaining.size} arrows left", depth)
            if (depth == 0) initiallyFree = free.size
            depth++
            for (a in free) {
                cellsById[a.id]!!.forEach { occupied.remove(it) }
                remaining.remove(a)
            }
        }
        return ValidationResult(true, "ok", depth, initiallyFree)
    }

    private fun isFree(arrow: ArrowModel, occupied: Map<GridPoint, String>, cols: Int, rows: Int): Boolean {
        var c = arrow.head.col + arrow.direction.dx
        var r = arrow.head.row + arrow.direction.dy
        while (c in 0 until cols && r in 0 until rows) {
            val who = occupied[GridPoint(c, r)]
            if (who != null && who != arrow.id) return false
            c += arrow.direction.dx
            r += arrow.direction.dy
        }
        return true
    }
}
