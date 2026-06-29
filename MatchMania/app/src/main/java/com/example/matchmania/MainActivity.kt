package com.example.matchmania

// ─────────────────────────────────────────────────────────────────────────────
// MainActivity.kt  —  the Compose UI (Part C).  STARTER FILE.
//
// This app already COMPILES AND RUNS as-is: launch it and you'll see the top bar,
// a stats line, a big "build the board here" placeholder, and three buttons. Your
// job is to turn the placeholder into a real, playable board.
//
//   ✅ PROVIDED FOR YOU: the Activity, the Scaffold + top bar, the hoisted board
//      state (C1), the stats line, and the three buttons already calling the right
//      logic functions (C4 wiring). A @Preview is set up too.
//   ✍️ YOUR JOB:
//        • C2 — replace `BoardPlaceholder(...)` with a real 4×4 LazyVerticalGrid of
//               tappable tiles whose look depends on each tile's state.
//        • C3 — show the "🎉 You win!" banner when board.isSolved() (and polish the
//               stats line if you like).
//        • Then finish the logic TODOs in MatchLogic.kt so tapping actually plays.
//
// Reminder: state flows DOWN (we pass `board` in), events flow UP (a tap calls
// back with an index, and THIS screen decides what it means: board = board.flip(i)).
// ─────────────────────────────────────────────────────────────────────────────

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchmania.ui.theme.MatchManiaTheme

// ── The Android entry point (PROVIDED) ───────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatchManiaTheme {
                MatchManiaScreen()
            }
        }
    }
}

// ── The screen: owns the state, lays everything out (mostly PROVIDED) ─────────
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3.
@Composable
fun MatchManiaScreen() {
    // C1 (PROVIDED): the board lives here as state. Reassigning `board` to a NEW
    // Board (what your MatchLogic functions return) makes the UI redraw.
    var board by remember { mutableStateOf(newBoard(DEFAULT_SYMBOLS)) }

    // A message the "Check" button updates (a simple bit of extra state).
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎮 Match Mania") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        // innerPadding already clears BOTH the top app bar/status bar and the
        // bottom navigation bar — apply it and you're done (no extra inset modifier).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Stats line (PROVIDED) — reads the helpers on Board.
            Text(
                text = "Pairs: ${board.pairsFound()} / $PAIR_COUNT   ·   Moves: ${board.moves}",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(12.dp))

            // ════════════════════════════════════════════════════════════════
            // C2 — TODO: replace this placeholder with the REAL board (see hw7).
            //   The 16 tiles as a 4-column grid of SQUARE, TAPPABLE cells; each
            //   cell must look different per tile.state, and a tap must produce a
            //   new board (board = board.flip(index)). Design the rest yourself.
            // ════════════════════════════════════════════════════════════════
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_SIZE),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(board.tiles) { index, tile ->
                    TileCell(tile = tile, onClick = { board = board.flip(index) })
                }
            }

            Spacer(Modifier.height(16.dp))

            // C3 — TODO: when the board is solved, show a celebratory "You win"
            //   banner here (include the move count). For now we only show the
            //   Check button's message, if any.
            if (board.isSolved()) {
                Text(
                    text = "🎉 You win! Solved in ${board.moves} moves.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            } else if (message.isNotEmpty()) {
                Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f)) // push the buttons to the bottom.

            // ── Action buttons (PROVIDED wiring — C4) ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // New Game → a brand-new shuffle (works once you implement B2).
                Button(
                    onClick = { board = newBoard(DEFAULT_SYMBOLS); message = "" },
                    modifier = Modifier.weight(1f),
                ) { Text("🔀 New Game") }

                // Reset → same layout, all face-down (works once you implement B4).
                OutlinedButton(
                    onClick = { board = board.reset(); message = "" },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
            }

            Spacer(Modifier.height(12.dp))

            // Check → report progress / win.
            Button(
                onClick = {
                    message = if (board.isSolved()) "You found all $PAIR_COUNT pairs! 🎉"
                              else "${PAIR_COUNT - board.pairsFound()} pairs to go."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Check", fontSize = 16.sp) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── A single tile in the grid ───────────────────────────────────────────
@Composable
private fun TileCell(tile: Tile, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f),
        border = BorderStroke(
            2.dp,
            if (tile.state == TileState.Matched) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = when (tile.state) {
                TileState.FaceDown -> MaterialTheme.colorScheme.surfaceVariant
                TileState.FaceUp -> MaterialTheme.colorScheme.surface
                TileState.Matched -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (tile.state != TileState.FaceDown) {
                Text(text = tile.face, fontSize = 28.sp)
            }
        }
    }
}

// ── Design-time preview (PROVIDED) ───────────────────────────────────────────
@Preview(name = "Match Mania (starter)", showBackground = true, showSystemUi = true)
@Composable
private fun MatchManiaPreview() {
    MatchManiaTheme {
        MatchManiaScreen()
    }
}
