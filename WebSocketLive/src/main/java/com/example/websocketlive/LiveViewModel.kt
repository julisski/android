// =============================================================================
// LiveViewModel.kt  —  collecting a Flow<ConnectionEvent> into one UI STATE
//
// CONCEPT THIS FILE TEACHES: turn the STREAM of socket events into a single,
// immutable [LiveUiState] the UI renders. The ViewModel collects the connection's
// events() Flow and folds each event into the state with a PURE reducer
// (reduceLiveState). Pulling the fold out into a top-level pure function makes the
// whole state machine unit-testable without any coroutines.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. LiveUiState — connection status + the running transcript of messages.
//   2. reduceLiveState(state, event) — the PURE event-folding function.
//   3. The ViewModel: collects events() in init; send() optimistically adds the
//      outgoing line, and the echoed reply arrives later as a MessageReceived.
// =============================================================================
package com.example.websocketlive

import androidx.lifecycle.ViewModel                          // base class that survives configuration changes
import androidx.lifecycle.viewModelScope                     // coroutine scope tied to this ViewModel's lifetime
import kotlinx.coroutines.flow.MutableStateFlow              // read/write reactive state holder
import kotlinx.coroutines.flow.StateFlow                     // read-only view exposed to the UI
import kotlinx.coroutines.flow.asStateFlow                   // exposes the mutable flow as immutable
import kotlinx.coroutines.flow.update                        // atomically transform the current state
import kotlinx.coroutines.launch                             // starts the event-collecting coroutine

// ===========================================================================
// STATE  —  one immutable snapshot the UI renders
// ===========================================================================

/**
 * LiveUiState — everything the screen needs, as one immutable value.
 *
 * @property status   the coarse connection status (header chip).
 * @property messages the transcript so far (oldest first), sent + received.
 */
data class LiveUiState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val messages: List<ChatMessage> = emptyList(),
)

/**
 * reduceLiveState — the PURE state machine. Given the current [state] and one
 * [event], return the next state. No coroutines, no side effects → trivially
 * unit-testable. The ViewModel below just applies this on every event.
 */
fun reduceLiveState(state: LiveUiState, event: ConnectionEvent): LiveUiState =
    when (event) {
        is ConnectionEvent.Connecting ->
            state.copy(status = ConnectionStatus.Connecting)

        is ConnectionEvent.Open ->
            state.copy(status = ConnectionStatus.Connected)

        is ConnectionEvent.MessageReceived ->
            // A frame arrived from the server → append it as a "not from me" line.
            state.copy(messages = state.messages + ChatMessage(event.text, fromMe = false))

        is ConnectionEvent.Closed ->
            state.copy(status = ConnectionStatus.Disconnected)

        is ConnectionEvent.Failure ->
            state.copy(status = ConnectionStatus.Failed)
    }

// ===========================================================================
// VIEWMODEL  —  owns the connection and the collected state
// ===========================================================================

/**
 * LiveViewModel — drives a [LiveConnection] and exposes the folded [LiveUiState]
 * as a [StateFlow] the screen observes.
 *
 * The connection is injected (defaulting to the offline fake) so the ViewModel never
 * hard-codes WHERE messages come from — and so tests can pass a controlled fake.
 *
 * @param connection the live connection; defaults to the offline [FakeWebSocketClient].
 */
class LiveViewModel(
    private val connection: LiveConnection = FakeWebSocketClient(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        // Collect the connection's event stream for the ViewModel's whole lifetime,
        // folding each event into the UI state with the pure reducer.
        viewModelScope.launch {
            connection.events().collect { event ->
                _uiState.update { current -> reduceLiveState(current, event) }
            }
        }
    }

    /** Open the connection. Status updates arrive via the events() stream. */
    fun connect() = connection.connect()

    /**
     * Send [text] to the server. We OPTIMISTICALLY append it as our own line right
     * away; the server's echo (a real reply) arrives later as a MessageReceived and
     * is appended by the reducer. Blank input is ignored.
     */
    fun send(text: String) {
        if (text.isBlank()) return
        connection.send(text)
        _uiState.update { it.copy(messages = it.messages + ChatMessage(text, fromMe = true)) }
    }

    /** Close the connection. */
    fun disconnect() = connection.close()
}
