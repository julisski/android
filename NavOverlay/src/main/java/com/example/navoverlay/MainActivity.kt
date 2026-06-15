// =============================================================================
// MainActivity.kt
//
// A list → detail sample whose SOLE TEACHING GOAL is the OVERLAY (dialog) SCENE
// in Navigation 3 (Nav3), and — crucially — HOW AN OVERLAY AFFECTS THE BACK STACK.
//
//   • Jetpack Compose        — the declarative UI toolkit (no XML layouts).
//   • Navigation 3 (Nav3)    — the modern, Compose-first navigation library.
//   • Kotlinx Serialization  — used to mark navigation keys @Serializable.
//
// ─── WHAT IS AN "OVERLAY" HERE? ──────────────────────────────────────────────
//
// A NORMAL destination REPLACES whatever was on screen: push DetailKey and the
// list screen is gone (only the top entry is composed). An OVERLAY destination is
// DIFFERENT: it draws ON TOP of the screen below it, and the screen below STAYS
// COMPOSED AND VISIBLE behind it. A dialog is the classic overlay — a card with a
// dimmed scrim, the previous screen showing through around/under it.
//
// ─── THE KEY IDEA: AN OVERLAY IS STILL A REGULAR KEY ON THE BACK STACK ────────
//
// This is the whole lesson. Opening the dialog is an ORDINARY push:
//       backStack.add(FactDialogKey(item.id))     // stack grows by 1
// and dismissing it is an ORDINARY pop:
//       backStack.removeLastOrNull()              // stack shrinks by 1
// So the back stack mechanics are EXACTLY the same as any other screen:
//
//       [PlanetsKey, DetailKey]                       ← viewing a planet (depth 2)
//   add(FactDialogKey)  ─►  [PlanetsKey, DetailKey, FactDialogKey]   (depth 3)
//   removeLastOrNull()  ─►  [PlanetsKey, DetailKey]                  (back to depth 2)
//
// What is NOT the same is the RENDERING. Because FactDialogKey's entry is marked
// as a dialog, Nav3 keeps the entry BELOW it (DetailKey) on screen and renders the
// dialog over it. System Back, tapping the scrim, and our "OK" button all do the
// same thing: pop that top key. That is why a dialog "feels" different yet needs
// no special back handling — it is just the top of the stack.
//
// ─── HOW NAV3 DECIDES "THIS KEY IS AN OVERLAY": SceneStrategy ─────────────────
//
// NavDisplay renders the back stack through a `sceneStrategy`. A SceneStrategy
// looks at the entries and may claim the top one(s) into a "Scene". We chain two:
//
//   sceneStrategy = DialogSceneStrategy<NavKey>() then SinglePaneSceneStrategy()
//
//   • DialogSceneStrategy      — if the TOP entry has dialog metadata, it renders
//                                that entry inside a Compose Dialog() AND keeps the
//                                entries beneath it (its `overlaidEntries`) drawn.
//                                Otherwise it returns null and defers...
//   • SinglePaneSceneStrategy  — ...to the normal full-screen one-entry strategy.
//
// `then` chains them (try dialog first, else single-pane). An entry opts IN to the
// dialog strategy by attaching DialogSceneStrategy.dialog() to its `metadata`:
//
//   entry<FactDialogKey>(metadata = DialogSceneStrategy.dialog()) { ... }
//
// (Discovered from navigation3-ui 1.0.1 sources: DialogSceneStrategy.calculateScene
//  reads DIALOG_KEY off the last entry's metadata and builds a DialogScene whose
//  overlaidEntries = entries.dropLast(1) — i.e. everything under the dialog stays.)
//
//   PlanetsScreen --tap--> DetailScreen --button--> [Fun-fact DIALOG over detail]
//   (all planets)         (one planet)             (overlay; detail still visible)
//
// NOTE: a dialog is interactive MOTION/STATE; the static @Preview pane shows each
// screen at rest. Run on a device/emulator and open the dialog to SEE the detail
// screen remaining behind it.
// =============================================================================

// The package declaration. Every class/function below lives in this namespace,
// which also matches the directory structure under src/main/java/.
package com.example.navoverlay

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                    // savedInstanceState type passed to onCreate
import android.util.Log                                     // Logcat logging (we print the stack on every frame)
import androidx.activity.ComponentActivity                  // base Activity class with Compose support
import androidx.activity.compose.setContent                 // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                    // lets the app draw behind the system bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.clickable                // makes a row tappable
import androidx.compose.foundation.layout.Arrangement       // controls spacing between a Row's children
import androidx.compose.foundation.layout.Column            // stacks children vertically
import androidx.compose.foundation.layout.Row               // lays children out horizontally (the dialog buttons)
import androidx.compose.foundation.layout.Spacer            // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize       // modifier: take all available width AND height
import androidx.compose.foundation.layout.fillMaxWidth      // modifier: take all available width
import androidx.compose.foundation.layout.height            // modifier: force a specific height
import androidx.compose.foundation.layout.padding           // modifier: add space around content
import androidx.compose.foundation.layout.width             // modifier: force a specific width (dialog sizing)
import androidx.compose.foundation.lazy.LazyColumn          // scrolling list (only renders visible rows)
import androidx.compose.foundation.lazy.items               // iterate a List inside a LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape // rounded corners for the dialog card

// --- Material 3 component imports ---------------------------------------------
import androidx.compose.material3.Button                    // filled, tappable button
import androidx.compose.material3.HorizontalDivider         // thin horizontal separator line
import androidx.compose.material3.MaterialTheme             // access to the current theme's colors/typography
import androidx.compose.material3.Scaffold                  // standard screen frame (handles insets, bars, etc.)
import androidx.compose.material3.Surface                   // a themed surface; we use it as the dialog "card"
import androidx.compose.material3.Text                      // draws text
import androidx.compose.material3.TextButton                // low-emphasis button (the dialog's "Jump to list")

// --- Compose runtime / tooling imports ---------------------------------------
import androidx.compose.runtime.Composable                  // marks a function as emitting UI
import androidx.compose.runtime.remember                    // remembers a value across recomposition (the sceneStrategy)
import androidx.compose.ui.Modifier                         // the "how to lay out / decorate" object
import androidx.compose.ui.tooling.preview.Preview          // enables @Preview rendering in Android Studio
import androidx.compose.ui.tooling.preview.PreviewParameter // feeds a value into a preview parameter
import androidx.compose.ui.unit.dp                          // density-independent pixel unit (e.g. 16.dp)

// --- Navigation 3 imports -----------------------------------------------------
// Navigation 3 is the modern, Compose-first navigation approach: a single Activity
// holds a back stack of "keys", and Compose swaps the screen whenever the top key
// changes. No Fragments, no Intents, no XML nav graph.
import androidx.navigation3.runtime.NavKey                  // marker interface every navigation key implements
import androidx.navigation3.runtime.entryProvider           // DSL that maps each key type to a screen
import androidx.navigation3.runtime.rememberNavBackStack    // creates + remembers the back stack across recomposition
import androidx.navigation3.scene.DialogSceneStrategy       // renders dialog-flagged entries as an OVERLAY (the star of this sample)
import androidx.navigation3.scene.SinglePaneSceneStrategy   // the normal "one full-screen entry" strategy (our fallback)
import androidx.navigation3.ui.NavDisplay                   // renders the current back stack AND owns the sceneStrategy

// --- App + misc imports -------------------------------------------------------
import com.example.navoverlay.ui.theme.NavOverlayTheme       // our app's Material theme wrapper (see Theme.kt)
import kotlinx.serialization.Serializable                   // makes Nav3 keys serializable (required by Nav3)

// Logcat tag string. Every log line from this app can be filtered in Logcat by
// searching for "NavOv" (for "Nav Overlay demo").
private const val TAG = "NavOv"

// ===========================================================================
// NAVIGATION KEYS
// Each screen is identified by a "key". A key both names the destination AND
// carries that destination's arguments. Nav3 requires keys to implement NavKey
// and (for state saving across process death) be @Serializable.
//
// IMPORTANT: NOTHING about FactDialogKey's TYPE marks it as an overlay. It is an
// ordinary NavKey, pushed and popped like any other. What makes it render as a
// dialog is the `metadata` we attach to its ENTRY (see entryProvider below) — not
// the key itself. The key is just an identifier sitting on the back stack.
// ===========================================================================

// `data object` = a singleton with generated equals()/toString(). There is only
// ever one list screen, so a single shared object is the right model.
@Serializable
data object PlanetsKey : NavKey                            // screen 1: the list of all planets (no arguments)

// `data class` because each detail screen differs by which planet it shows.
@Serializable
data class DetailKey(val itemId: Int) : NavKey            // screen 2: one planet's detail

// `data class` because each dialog differs by which planet's fact it shows. This
// is the OVERLAY key — pushed onto the SAME stack as the screens above it.
@Serializable
data class FactDialogKey(val itemId: Int) : NavKey        // the overlay: a fun-fact dialog drawn OVER the detail

/**
 * MainActivity — the app's single Activity and the entry point Android launches.
 *
 * In a Nav3 app you typically have exactly one Activity; it hosts the Compose UI
 * and the navigation back stack, and Compose (not the Activity system) swaps —
 * and OVERLAYS — between the screens.
 */
class MainActivity : ComponentActivity() {
    // onCreate runs once when the Activity is first created. This is where we
    // install the Compose UI tree as the Activity's content.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)                 // always call through to the framework first
        enableEdgeToEdge()                                 // draw under the status/navigation bars for a modern look
        setContent {                                       // everything inside is the Compose UI
            // Apply our app theme (colors, typography, dark/light handling).
            NavOverlayTheme {
                // Scaffold provides the standard screen structure and, crucially,
                // hands us `innerPadding` — the space taken by system bars — so
                // our content isn't drawn underneath them.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * AppNavigation — owns the back stack, WIRES UP THE DIALOG OVERLAY via a
 * SceneStrategy, and maps each key to its screen.
 *
 * The whole point of the sample lives in two places below:
 *   1. `sceneStrategy = DialogSceneStrategy() then SinglePaneSceneStrategy()`
 *   2. the FactDialogKey entry carrying `metadata = DialogSceneStrategy.dialog()`
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    // The back stack is the list of keys currently "stacked", bottom to top.
    // rememberNavBackStack seeds it with PlanetsKey and preserves it across
    // recompositions (and configuration changes). Pushing a key navigates
    // FORWARD; popping a key navigates BACK — INCLUDING the dialog overlay.
    val backStack = rememberNavBackStack(PlanetsKey)

    // ── "HOW IT AFFECTS THE STACK", made visible ──
    // Print the stack contents on EVERY recomposition so you can watch in Logcat
    // (filter by "NavOv") exactly how opening/closing the dialog changes depth:
    //   stack(2): [PlanetsKey, DetailKey(3)]                       ← on the detail
    //   stack(3): [PlanetsKey, DetailKey(3), FactDialogKey(3)]     ← dialog open (+1)
    //   stack(2): [PlanetsKey, DetailKey(3)]                       ← dialog dismissed (-1)
    Log.d(TAG, "stack(${backStack.size}): ${backStack.toList()}")

    // ── THE SCENE STRATEGY (the feature of this sample) ──
    // remember it so the SAME strategy instance is reused across recompositions.
    //   • DialogSceneStrategy : claims a top entry that has dialog() metadata and
    //                           renders it as an OVERLAY (a Compose Dialog) while
    //                           keeping the entry beneath it on screen.
    //   • then SinglePaneSceneStrategy() : the fallback for every NON-dialog entry,
    //                           which renders one full-screen entry as usual.
    // Order matters: the dialog strategy must come FIRST so it gets first refusal
    // on the top entry; only if it returns null does the single-pane one take over.
    val sceneStrategy = remember {
        DialogSceneStrategy<NavKey>() then SinglePaneSceneStrategy()
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // System back / hardware back pops the top key. When a dialog is on top,
        // this pops the DIALOG (closing it); otherwise it pops a screen. Tapping
        // the dimmed scrim outside the dialog routes here too, via the strategy.
        onBack = { backStack.removeLastOrNull() },
        // Hand NavDisplay our chained strategy. This single line is what enables
        // dialog overlays; without it, NavDisplay would render FactDialogKey as a
        // plain full-screen replacement (covering the detail) instead of over it.
        sceneStrategy = sceneStrategy,
        entryProvider = entryProvider {
            // SCREEN 1 — the list of all planets. Tapping a planet does a NORMAL
            // push to DetailKey: a full-screen REPLACE (the list disappears). This
            // is the deliberate contrast to the overlay below.
            entry<PlanetsKey> {
                PlanetsScreen(
                    items = sampleItems,
                    onOpen = { itemId -> backStack.add(DetailKey(itemId)) }   // normal push (replace)
                )
            }

            // SCREEN 2 — one planet's detail. From here the "Show fun fact" button
            // pushes the OVERLAY key. Note: it is the SAME backStack.add(...) call
            // shape as a normal navigation — the only thing that makes it an overlay
            // is the metadata on the FactDialogKey entry below.
            entry<DetailKey> { key ->
                DetailScreen(
                    item = itemById(key.itemId),
                    onShowFact = { itemId -> backStack.add(FactDialogKey(itemId)) }, // push overlay (stack +1)
                    onBack = { backStack.removeLastOrNull() }                        // pop detail (back to list)
                )
            }

            // THE OVERLAY — a fun-fact dialog drawn OVER the detail screen.
            //
            // `metadata = DialogSceneStrategy.dialog()` is the opt-in: it tags this
            // entry so DialogSceneStrategy claims it and renders FactDialog inside a
            // Compose Dialog (dimmed scrim + the detail screen visible behind it),
            // instead of the single-pane full-screen replace every other entry gets.
            //
            // dialog() also accepts DialogProperties; the default already enables
            // dismiss-on-back-press and dismiss-on-tap-outside, both of which route
            // through NavDisplay.onBack above and POP this key — so the dialog needs
            // no special dismissal code of its own.
            entry<FactDialogKey>(metadata = DialogSceneStrategy.dialog()) { key ->
                FactDialog(
                    item = itemById(key.itemId),
                    // "OK" closes the dialog: a plain pop (stack -1) back to detail.
                    onDismiss = { backStack.removeLastOrNull() },
                    // "Jump to list" shows an overlay button can manipulate the stack
                    // arbitrarily: here we pop EVERYTHING above the root, so both the
                    // dialog AND the detail go away and we land back on the list.
                    onJumpToList = { while (backStack.size > 1) backStack.removeLastOrNull() }
                )
            }
        }
    )
}

/**
 * Planets screen (SCREEN 1): a scrolling list of every planet; tapping a row calls
 * [onOpen] with its id (which the host turns into a normal, full-screen push).
 *
 * This composable is intentionally "dumb" — it knows nothing about navigation or
 * overlays. It only renders the planets it's given and reports taps via [onOpen].
 *
 * @param items    the rows to render.
 * @param onOpen   invoked with a planet's id when its row is tapped.
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun PlanetsScreen(
    items: List<Item>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // LazyColumn is the Compose equivalent of a RecyclerView: it only composes and
    // lays out the rows currently visible on screen, so long lists stay fast.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items) { item ->                              // draw one row per planet
            Column(
                modifier = Modifier
                    .fillMaxWidth()                         // row spans the full width...
                    .clickable { onOpen(item.id) }          // ...and the WHOLE row is tappable -> open detail
                    .padding(16.dp)                         // breathing room inside the row
            ) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(text = item.blurb, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()                             // thin line between rows
        }
    }
}

/**
 * Detail screen (SCREEN 2): shows the single [item] resolved from the id in the
 * nav key, with a button that opens the OVERLAY dialog and a button that pops back.
 *
 * @param item       the fully-resolved planet to display.
 * @param onShowFact invoked with the item's id when "Show fun fact" is tapped
 *                   (the host pushes the overlay key).
 * @param onBack     invoked when the user taps "Back to list".
 * @param modifier   optional layout modifier supplied by the caller.
 */
@Composable
fun DetailScreen(
    item: Item,
    onShowFact: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Large headline showing the planet's title.
        Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        // The planet's longer description.
        Text(text = item.blurb, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        // Primary action: open the fun-fact OVERLAY. Watch the detail screen stay
        // visible behind the dialog when this is tapped.
        Button(onClick = { onShowFact(item.id) }) {
            Text("Show fun fact")
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Secondary action: pop back to the list. (System back does the same.)
        Button(onClick = onBack) {
            Text("Back to list")
        }
    }
}

/**
 * Fun-fact dialog (THE OVERLAY): the content rendered INSIDE Nav3's Dialog when
 * FactDialogKey is on top of the stack. Because DialogSceneStrategy wraps this in a
 * Compose Dialog, we only need to draw the "card" — Nav3 supplies the dimmed scrim
 * and keeps the detail screen composed behind us.
 *
 * @param item         the planet whose fun fact to show.
 * @param onDismiss    invoked by "OK" (pops just this dialog → back to detail).
 * @param onJumpToList invoked by "Jump to list" (pops to the root → back to list).
 * @param modifier     optional layout modifier supplied by the caller.
 */
@Composable
fun FactDialog(
    item: Item,
    onDismiss: () -> Unit,
    onJumpToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Surface gives the dialog a themed, rounded "card" look. The Dialog window
    // around it (with its scrim) is provided by Nav3's DialogSceneStrategy.
    Surface(
        modifier = modifier.width(300.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,                              // subtle lift so the card reads above the scrim
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Headline naming which planet this fun fact is about.
            Text(
                text = "Fun fact about ${item.title}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // The fun fact itself, pulled from the item's `fact` field.
            Text(text = item.fact, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            // A row of two actions that demonstrate two DIFFERENT stack effects.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,    // align buttons to the trailing edge
            ) {
                // Pops to the root: dialog AND detail both removed.
                TextButton(onClick = onJumpToList) {
                    Text("Jump to list")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Pops just this dialog: back to the detail, unchanged, underneath.
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}

// ===========================================================================
// @Preview functions — render screen states in the design pane.
//
// IMPORTANT FOR THIS SAMPLE: @Preview renders ONE STATIC frame and does NOT run
// the navigator, so it cannot show the dialog actually floating OVER the detail
// screen with the detail visible behind it. The previews below just sanity-check
// each piece in isolation; run the app to see the real overlay behavior.
// ===========================================================================

// SCREEN 1 — the list of planets.
@Preview(name = "Planets", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun PlanetsScreenPreview() {
    NavOverlayTheme {
        PlanetsScreen(items = sampleItems, onOpen = {})
    }
}

// SCREEN 2 — one compact card per planet, so every detail is previewed.
@Preview(name = "Detail", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun DetailScreenPreview(
    @PreviewParameter(ItemPreviewProvider::class) item: Item
) {
    NavOverlayTheme {
        DetailScreen(item = item, onShowFact = {}, onBack = {})
    }
}

// THE OVERLAY card in isolation (here it renders as a plain card, since there is
// no Dialog window or detail screen behind it outside the running app).
@Preview(name = "Fact dialog", showBackground = true, widthDp = 320, heightDp = 320)
@Composable
fun FactDialogPreview() {
    NavOverlayTheme {
        FactDialog(item = sampleItems.first(), onDismiss = {}, onJumpToList = {})
    }
}
