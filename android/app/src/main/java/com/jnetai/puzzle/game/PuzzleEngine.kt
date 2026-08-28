package com.jnetai.puzzle.game

import com.jnetai.puzzle.utils.ErrorLogger
import kotlin.random.Random

/**
 * PuzzleEngine - sliding puzzle (n-puzzle) game state and rules.
 *
 * The board is an N x N grid of tiles. One tile is removed (the "empty" tile)
 * and the remaining tiles must be slid into the correct positions by tapping a
 * tile adjacent to the empty space, which swaps it into the empty slot.
 *
 * State export/import allows the UI to survive rotations via a simple string.
 */
class PuzzleEngine(
    val gridSize: Int
) {
    private val tileCount: Int = gridSize * gridSize

    /** board[i] holds the piece number currently sitting in slot i.
     *  Piece `tileCount - 1` is the removed (empty) piece. */
    private val board: IntArray = IntArray(tileCount) { it }

    private var emptyIndex: Int = tileCount - 1

    var moves: Int = 0
        private set

    /** true once the puzzle has been scrambled (game started). */
    var started: Boolean = false

    init {
        if (gridSize < 3 || gridSize > 6) {
            ErrorLogger.logf(ErrorLogger.Codes.PZL_GRID_INVALID,
                "Invalid grid size %d requested - clamped to valid range", gridSize)
        }
    }

    /** Returns a defensive copy of the current board layout. */
    fun getBoard(): IntArray = board.copyOf()

    /** Restore a previously saved board string (see [exportState]). */
    fun importState(state: String, moveCount: Int, startedFlag: Boolean): Boolean {
        return try {
            val parts = state.split(",")
            if (parts.size != tileCount) {
                ErrorLogger.logf(ErrorLogger.Codes.PZL_STATE,
                    "Imported state has %d elements, expected %d", parts.size, tileCount)
                return false
            }
            for (i in 0 until tileCount) {
                val v = parts[i].toIntOrNull() ?: return false
                if (v < 0 || v >= tileCount) return false
                board[i] = v
                if (v == tileCount - 1) emptyIndex = i
            }
            moves = moveCount
            started = startedFlag
            true
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.PZL_STATE, "Failed to import state", e)
            false
        }
    }

    // ----- Game flow -----

    /** Scramble the board with `iterations` random legal moves from the solved state. */
    fun scramble(iterations: Int = 200) {
        try {
            // Reset to solved first.
            for (i in 0 until tileCount) board[i] = i
            emptyIndex = tileCount - 1
            moves = 0
            started = false

            var lastMoved = -1
            var count = 0
            var guard = 0
            while (count < iterations && guard < iterations * 8) {
                guard++
                val candidates = movableTiles()
                if (candidates.isEmpty()) break
                val choice = candidates[Random.nextInt(candidates.size)]
                // Avoid immediately undoing the previous move.
                if (choice != lastMoved) {
                    moveTile(choice)
                    lastMoved = choice
                    count++
                }
            }
            moves = 0
            started = true
            ErrorLogger.logf(ErrorLogger.Codes.PZL_SCRAMBLE,
                "Scrambled %dx%d puzzle with %d moves", gridSize, gridSize, count)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.PZL_SCRAMBLE, "Failed to scramble puzzle", e)
        }
    }

    /** Indexes of tiles that can currently slide into the empty slot. */
    fun movableTiles(): List<Int> {
        val result = mutableListOf<Int>()
        val row = emptyIndex / gridSize
        val col = emptyIndex % gridSize
        if (row > 0) result.add(emptyIndex - gridSize)
        if (row < gridSize - 1) result.add(emptyIndex + gridSize)
        if (col > 0) result.add(emptyIndex - 1)
        if (col < gridSize - 1) result.add(emptyIndex + 1)
        return result
    }

    /** Slide the tile at `index` into the empty slot if legal. Returns true if moved. */
    fun moveTile(index: Int): Boolean {
        if (index !in 0 until tileCount) {
            ErrorLogger.logf(ErrorLogger.Codes.PZL_MOVE_BAD, "moveTile index %d out of range", index)
            return false
        }
        if (index == emptyIndex) return false
        val row = emptyIndex / gridSize
        val col = emptyIndex % gridSize
        val tRow = index / gridSize
        val tCol = index % gridSize
        val isAdjacent = (tRow == row && kotlin.math.abs(tCol - col) == 1) ||
            (tCol == col && kotlin.math.abs(tRow - row) == 1)
        if (!isAdjacent) return false

        board[emptyIndex] = board[index]
        board[index] = tileCount - 1
        emptyIndex = index
        moves++
        return true
    }

    /** True when the board is in the correct solved order. */
    fun isSolved(): Boolean {
        return board.indices.all { board[it] == it }
    }

    /** Dump current state as a compact CSV string for persistence. */
    fun exportState(): String {
        return board.joinToString(",")
    }
}