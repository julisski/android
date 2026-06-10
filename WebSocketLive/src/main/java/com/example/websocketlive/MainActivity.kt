// =============================================================================
// MainActivity.kt  —  a live, full-duplex WebSocket chat surface
//
// CONCEPT THIS PROJECT TEACHES: a SECOND network protocol — WebSocket — for
// backend communication. Where the NetworkParsing demo does HTTP request/response
// (ask once, get one answer), this keeps a single connection open and streams
// messages BOTH ways. The UI shows a live connection status, a running transcript,
// and a box to send text that the echo server sends straight back.
//
// This file is the UI layer; the rest is split across:
//   • ConnectionEvent.kt  — ConnectionEvent (wire events) + ChatMessage/ConnectionStatus.
//   • WebSocketClient.kt   — LiveConnection: REAL (OkHttp WebSocket) vs FAKE + the switch.
//   • LiveViewModel.kt     — LiveUiState + the pure reduceLiveState + the ViewModel.
//
// WHAT THE STUDENT SHOULD INSPECT IN THIS FILE:
//   1. LiveScreen() — collects the ViewModel's StateFlow and opens the connection
//      once via LaunchedEffect; owns the text-field input state.
//   2. LiveContent() — a STATELESS overload that renders a LiveUiState, so the
//      @Previews can drive it with hand-made state (no real socket/ViewModel).
// =============================================================================
package com.example.websocketlive

import android.os.Bundle                                     // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                   // base Activity class with Compose support
import androidx.activity.compose.setContent                  // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                    // lets the app draw behind the system bars
import androidx.compose.foundation.layout.Arrangement        // spacing between children in a row/column
import androidx.compose.foundation.layout.Column             // stacks children vertically
import androidx.compose.foundation.layout.Row                // lays children out horizontally
import androidx.compose.foundation.layout.Spacer             // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize        // modifier: take all available space
import androidx.compose.foundation.layout.fillMaxWidth       // modifier: take all available width
import androidx.compose.foundation.layout.padding            // modifier: add space around content
import androidx.compose.foundation.layout.width              // modifier: force a specific width
import androidx.compose.foundation.lazy.LazyColumn           // scrolling list (only renders visible rows)
import androidx.compose.foundation.lazy.items                // iterate a List inside a LazyColumn
import androidx.compose.material3.Button                     // filled, tappable button
import androidx.compose.material3.HorizontalDivider          // thin separator line
import androidx.compose.material3.MaterialTheme              // access to the theme's colors/typography
import androidx.compose.material3.OutlinedTextField          // the message input box
import androidx.compose.material3.Scaffold                   // standard screen frame (handles insets)
import androidx.compose.material3.Text                       // draws text
import androidx.compose.runtime.Composable                   // marks a function as emitting UI
import androidx.compose.runtime.LaunchedEffect               // run a side effect (connect) once on entry
import androidx.compose.runtime.getValue                     // enables `val x by …` delegation for State
import androidx.compose.runtime.mutableStateOf               // creates observable UI state
import androidx.compose.runtime.remember                     // keeps state alive across recompositions
import androidx.compose.runtime.setValue                     // enables reassigning `by` state with `=`
import androidx.compose.ui.Alignment                         // align children within a layout
import androidx.compose.ui.Modifier                          // the "how to lay out / decorate" object
import androidx.compose.ui.text.style.TextAlign              // align text inside its own box
import androidx.compose.ui.tooling.preview.Preview           // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                           // density-independent pixel unit (e.g. 16.dp)
import androidx.lifecycle.compose.collectAsStateWithLifecycle // observe a StateFlow safely w.r.t. lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel        // obtain a ViewModel from inside a @Composable
import com.example.websocketlive.ui.theme.WebSocketLiveTheme // our app's Material theme wrapper

/**
 * MainActivity — the app's single Activity and the entry point Android launches.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebSocketLiveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LiveScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ===========================================================================
// UI  —  STATEFUL screen: opens the connection, owns the input text
// ===========================================================================

/**
 * LiveScreen — the STATEFUL entry composable. It obtains the [LiveViewModel],
 * observes its StateFlow, opens the connection once on entry, owns the text-field
 * input, and delegates drawing to the stateless [LiveContent].
 */
@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = viewModel(),
) {
    // THREE ideas in one line —  val X by someStateFlow.collectAsStateWithLifecycle():
    //   1. someStateFlow                  — a StateFlow the ViewModel exposes (its live value).
    //   2. .collectAsStateWithLifecycle() — SUBSCRIBES to it and wraps the latest value in a
    //        Compose State, so this composable RECOMPOSES whenever the flow emits a new value.
    //        "WithLifecycle" = collect only while the screen is visible (lifecycle >= STARTED)
    //        and PAUSE off-screen; the Android-safe version of a plain collectAsState().
    //   3. `by`                           — a property delegate that unwraps the State's .value,
    //        so X reads as the plain value directly (no .value). Reading X also registers this
    //        composable as a reader, which is what makes Compose recompose it on the next emit.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Open the connection exactly once when the screen first appears. Using Unit as
    // the key means this runs a single time, not on every recomposition.
    LaunchedEffect(Unit) { viewModel.connect() }

    // The current text in the input box, kept across recompositions.
    var input by remember { mutableStateOf("") }

    LiveContent(
        uiState = uiState,
        input = input,
        onInputChange = { input = it },
        onSend = {
            viewModel.send(input)
            input = ""                                   // clear the box after sending
        },
        modifier = modifier,
    )
}

// ===========================================================================
// UI  —  STATELESS renderer: a pure function of (state, input)
// ===========================================================================

/**
 * LiveContent — the STATELESS UI. Given the [uiState] and the current [input] text,
 * it draws the status chip, the transcript, and the input row, and reports edits and
 * sends through callbacks. It holds NO state and builds NO socket/ViewModel — which
 * is why the @Previews can drive it directly.
 */
@Composable
fun LiveContent(
    uiState: LiveUiState,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "WebSocket Live", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.width(8.dp))

        // Connection status chip — colour reflects the coarse state.
        Text(
            text = "Status: ${uiState.status}",
            style = MaterialTheme.typography.titleMedium,
            color = when (uiState.status) {
                ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
                ConnectionStatus.Failed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // The transcript fills the space between the header and the input row.
        // weight(1f) makes the list take all remaining vertical space.
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(uiState.messages) { message ->
                MessageRow(message)
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        // The input row: a text box + a Send button.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                label = { Text("Message") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Disabled while there is nothing to send (blank input).
            Button(onClick = onSend, enabled = input.isNotBlank()) {
                Text("Send")
            }
        }
    }
}

/**
 * MessageRow — one transcript line. Messages we sent are right-aligned in the
 * primary colour; messages from the server are left-aligned in the default colour.
 */
@Composable
private fun MessageRow(message: ChatMessage) {
    Text(
        text = (if (message.fromMe) "you: " else "server: ") + message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (message.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        textAlign = if (message.fromMe) TextAlign.End else TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

// ===========================================================================
// PREVIEWS  —  render the transcript with hand-supplied state (no real socket)
// ===========================================================================

private val previewState = LiveUiState(
    status = ConnectionStatus.Connected,
    messages = listOf(
        ChatMessage("Connected to the OFFLINE fake echo server.", fromMe = false),
        ChatMessage("hello", fromMe = true),
        ChatMessage("hello", fromMe = false),
    ),
)

@Preview(name = "Connected", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LiveContentConnectedPreview() {
    WebSocketLiveTheme {
        LiveContent(uiState = previewState, input = "next message", onInputChange = {}, onSend = {})
    }
}

@Preview(name = "Connecting (empty)", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LiveContentConnectingPreview() {
    WebSocketLiveTheme {
        LiveContent(
            uiState = LiveUiState(status = ConnectionStatus.Connecting),
            input = "",
            onInputChange = {},
            onSend = {},
        )
    }
}
