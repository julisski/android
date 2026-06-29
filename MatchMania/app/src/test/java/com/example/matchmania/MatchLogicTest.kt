package com.example.matchmania

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// MatchLogicTest.kt  —  Part D.  STARTER FILE.
//
// These run on a plain JVM (no emulator): right-click ▸ Run, or
//   ./gradlew testDebugUnitTest
//
// Two example tests are PROVIDED and already pass (they only check structure that
// is true even before you finish the logic). Below them is your TODO list — write
// the remaining tests. As you implement newBoard/flip/reset in MatchLogic.kt, your
// tests should turn green; while a function is still a stub, its test will fail —
// that's the test doing its job.
// ─────────────────────────────────────────────────────────────────────────────
class MatchLogicTest {

    // ── PROVIDED example #1: a fresh board has the right number of tiles. ──────
    @Test
    fun newBoard_has16Tiles() {
        assertEquals(CELL_COUNT, newBoard(DEFAULT_SYMBOLS).tiles.size)
    }

    // ── PROVIDED example #2: a fresh board is all face-down and not yet solved. ─
    @Test
    fun newBoard_startsUnsolvedAndFaceDown() {
        val board = newBoard(DEFAULT_SYMBOLS)
        assertEquals(0, board.moves)
        assertFalse(board.isSolved())
        assertTrue(board.tiles.all { it.state == TileState.FaceDown })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ✍️  YOUR TESTS (Part D)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun composition_hasCorrectPairs() {
        val board = newBoard(DEFAULT_SYMBOLS)
        val faceCounts = board.tiles.groupBy { it.face }.mapValues { it.value.size }
        assertEquals(PAIR_COUNT, faceCounts.size)
        faceCounts.values.forEach { assertEquals(2, it) }
    }

    @Test
    fun determinism_sameSeedGivesSameBoard() {
        val seed = 123L
        val board1 = newBoard(DEFAULT_SYMBOLS, Random(seed))
        val board2 = newBoard(DEFAULT_SYMBOLS, Random(seed))
        assertEquals(board1.tiles, board2.tiles)
    }

    @Test
    fun flip_revealsFaceDownTile() {
        val board = newBoard(DEFAULT_SYMBOLS)
        val flipped = board.flip(0)
        assertEquals(TileState.FaceUp, flipped.tiles[0].state)
        assertEquals(0, flipped.moves)
    }

    @Test
    fun flip_matchesTwinTiles() {
        val board = newBoard(DEFAULT_SYMBOLS, Random(42))
        val firstIndex = 0
        val firstFace = board.tiles[firstIndex].face
        val twinIndex = board.tiles.indices.first { it != firstIndex && board.tiles[it].face == firstFace }

        val board1 = board.flip(firstIndex)
        val board2 = board1.flip(twinIndex)

        assertEquals(TileState.Matched, board2.tiles[firstIndex].state)
        assertEquals(TileState.Matched, board2.tiles[twinIndex].state)
        assertEquals(1, board2.moves)
    }

    @Test
    fun flip_mismatchLeavesFaceUp_thenNextFlipClears() {
        val board = newBoard(DEFAULT_SYMBOLS, Random(42))
        val firstIndex = 0
        val firstFace = board.tiles[firstIndex].face
        val mismatchIndex = board.tiles.indices.first { board.tiles[it].face != firstFace }

        val board1 = board.flip(firstIndex)
        val board2 = board1.flip(mismatchIndex)

        assertEquals(TileState.FaceUp, board2.tiles[firstIndex].state)
        assertEquals(TileState.FaceUp, board2.tiles[mismatchIndex].state)
        assertEquals(1, board2.moves)

        val thirdIndex = board.tiles.indices.first { it != firstIndex && it != mismatchIndex }
        val board3 = board2.flip(thirdIndex)

        assertEquals(TileState.FaceDown, board3.tiles[firstIndex].state)
        assertEquals(TileState.FaceDown, board3.tiles[mismatchIndex].state)
        assertEquals(TileState.FaceUp, board3.tiles[thirdIndex].state)
        assertEquals(1, board3.moves)
    }

    @Test
    fun flip_returnsNewBoard() {
        val board = newBoard(DEFAULT_SYMBOLS)
        val flipped = board.flip(0)
        assertTrue(board !== flipped)
        assertEquals(TileState.FaceDown, board.tiles[0].state)
    }

    @Test
    fun winAndReset_works() {
        var board = newBoard(DEFAULT_SYMBOLS, Random(42))
        val symbols = board.tiles.map { it.face }.distinct()
        symbols.forEach { symbol ->
            val indices = board.tiles.indices.filter { board.tiles[it].face == symbol }
            board = board.flip(indices[0]).flip(indices[1])
        }

        assertTrue(board.isSolved())
        assertEquals(PAIR_COUNT, board.pairsFound())

        val resetBoard = board.reset()
        assertFalse(resetBoard.isSolved())
        assertEquals(0, resetBoard.moves)
        assertTrue(resetBoard.tiles.all { it.state == TileState.FaceDown })
        assertEquals(board.tiles.map { it.face }, resetBoard.tiles.map { it.face })
    }
}
