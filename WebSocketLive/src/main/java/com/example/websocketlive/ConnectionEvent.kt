// =============================================================================
// ConnectionEvent.kt  —  the events a live connection emits + the UI models
//
// CONCEPT THIS FILE TEACHES: a WebSocket is a STREAM, not a single request. Unlike
// the request/response of HTTP (see the NetworkParsing demo), a socket pushes a
// SEQUENCE of things over time: it opens, messages arrive, it may fail or close.
// We model that sequence as a sealed [ConnectionEvent] type and let the connection
// expose a Flow<ConnectionEvent> the ViewModel collects.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. ConnectionEvent — the closed set of things that can happen on the wire.
//   2. ConnectionStatus — the coarse status the UI shows (a header chip).
//   3. ChatMessage — one line in the transcript, tagged with who sent it.
// =============================================================================
package com.example.websocketlive

// ===========================================================================
// WIRE EVENTS  —  what the connection reports as it happens
// ===========================================================================

/**
 * ConnectionEvent — the complete set of things a live connection can report.
 * Sealed, so a `when` reducing it into UI state is exhaustive.
 */
sealed interface ConnectionEvent {
    /** The socket handshake has started. */
    data object Connecting : ConnectionEvent

    /** The socket is open and ready to send/receive. */
    data object Open : ConnectionEvent

    /**
     * A text frame arrived FROM the server.
     * @property text the received payload.
     */
    data class MessageReceived(val text: String) : ConnectionEvent

    /**
     * The socket closed cleanly.
     * @property reason a human-readable close reason.
     */
    data class Closed(val reason: String) : ConnectionEvent

    /**
     * The socket failed (network dropped, handshake rejected, ...).
     * @property error a human-readable failure message.
     */
    data class Failure(val error: String) : ConnectionEvent
}

// ===========================================================================
// UI MODELS  —  what the screen renders
// ===========================================================================

/**
 * ConnectionStatus — the coarse connection state shown as a header chip. Derived
 * from the stream of [ConnectionEvent]s by the reducer in LiveViewModel.
 */
enum class ConnectionStatus { Disconnected, Connecting, Connected, Failed }

/**
 * ChatMessage — one line in the transcript.
 *
 * @property text   the message text.
 * @property fromMe true if WE sent it; false if it arrived from the server. The UI
 *   uses this to align/label sent vs received messages.
 */
data class ChatMessage(val text: String, val fromMe: Boolean)
