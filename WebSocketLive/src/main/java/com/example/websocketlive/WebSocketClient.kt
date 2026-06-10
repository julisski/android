// =============================================================================
// WebSocketClient.kt  —  the LIVE CONNECTION: one abstraction, REAL vs FAKE
//
// CONCEPT THIS FILE TEACHES: speaking a SECOND network protocol — the WebSocket
// protocol (ws:// / wss://) — which, unlike HTTP request/response, keeps a single
// FULL-DUPLEX connection open so either side can push messages at any time. We hide
// the protocol behind a small [LiveConnection] interface that exposes:
//   • events() : Flow<ConnectionEvent>  — everything the socket reports, as a stream
//   • connect() / send(text) / close()  — the actions a screen can take
//
// Behind the interface sits either:
//   • RealWebSocketClient — OkHttp's WebSocket + WebSocketListener against a public
//     echo server (wss://echo.websocket.events), or
//   • FakeWebSocketClient — emits a scripted open + echoes whatever you send, with
//     NO network, so the app and unit tests run completely offline.
//
// THE SWITCH provideLiveConnection(useFake = ...) chooses between them in one place
// (the same pattern the NetworkParsing demo uses for online vs offline). Default is
// FAKE so the project builds, runs, and tests everywhere.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. The LiveConnection interface — actions + an events() Flow.
//   2. RealWebSocketClient — how a WebSocketListener's callbacks (onOpen, onMessage,
//      onClosing, onFailure) are funnelled into a MutableSharedFlow of events.
//   3. FakeWebSocketClient — deterministic, offline; send() echoes back like a real
//      echo server, so the UI behaves identically without a network.
// =============================================================================
package com.example.websocketlive

import kotlinx.coroutines.flow.Flow                          // the stream type the connection exposes
import kotlinx.coroutines.flow.MutableSharedFlow             // hot stream we push socket events into
import kotlinx.coroutines.flow.asSharedFlow                  // expose the mutable flow as read-only
import okhttp3.OkHttpClient                                  // the HTTP/WebSocket engine
import okhttp3.Request                                       // describes the connection (the wss:// URL)
import okhttp3.Response                                      // handshake response passed to onOpen/onFailure
import okhttp3.WebSocket                                     // the open socket handle (send/close)
import okhttp3.WebSocketListener                             // receives the socket's lifecycle callbacks

// ===========================================================================
// CONNECTION CONTRACT  —  what the ViewModel depends on
// ===========================================================================

/**
 * LiveConnection — the abstraction the ViewModel talks to. It hides whether a real
 * WebSocket or an in-memory fake is behind it.
 */
interface LiveConnection {
    /**
     * The stream of [ConnectionEvent]s the connection reports (open, messages,
     * close, failure). A hot stream: collect it to observe what the socket is doing.
     */
    fun events(): Flow<ConnectionEvent>

    /** Open the connection (asynchronous; watch [events] for Connecting/Open). */
    fun connect()

    /** Send a text frame to the server. No-op if not currently open. */
    fun send(text: String)

    /** Close the connection cleanly. */
    fun close()
}

// ===========================================================================
// REAL IMPLEMENTATION  —  OkHttp WebSocket -> a Flow of events
// ===========================================================================

/**
 * RealWebSocketClient — opens a real WebSocket with OkHttp and forwards every
 * lifecycle callback into [events] as a [ConnectionEvent]. Requires the INTERNET
 * permission and a live connection at runtime.
 *
 * @property url        the wss:// endpoint (defaults to a public echo server).
 * @property httpClient the OkHttp engine (defaulted; injectable for testing).
 */
class RealWebSocketClient(
    private val url: String = "wss://echo.websocket.events",
    private val httpClient: OkHttpClient = OkHttpClient(),
) : LiveConnection {

    // The currently open socket, or null when disconnected. Held so send()/close() work.
    private var webSocket: WebSocket? = null

    // Hot stream of events. extraBufferCapacity lets the socket's callbacks emit with
    // tryEmit() (never suspending on the OkHttp thread) without dropping events.
    private val events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)

    override fun events(): Flow<ConnectionEvent> = events.asSharedFlow()

    override fun connect() {
        events.tryEmit(ConnectionEvent.Connecting)
        val request = Request.Builder().url(url).build()
        // newWebSocket starts the handshake; the listener below receives every callback.
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                events.tryEmit(ConnectionEvent.Open)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                events.tryEmit(ConnectionEvent.MessageReceived(text))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Acknowledge the close handshake, then report it.
                webSocket.close(NORMAL_CLOSURE, null)
                events.tryEmit(ConnectionEvent.Closed(reason.ifBlank { "closed" }))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                events.tryEmit(ConnectionEvent.Failure(t.message ?: "connection failed"))
            }
        })
    }

    override fun send(text: String) {
        webSocket?.send(text)                  // OkHttp queues the frame on its own thread
    }

    override fun close() {
        webSocket?.close(NORMAL_CLOSURE, "client closed")
        webSocket = null
    }

    private companion object {
        // 1000 is the WebSocket protocol's "normal closure" status code.
        const val NORMAL_CLOSURE = 1000
    }
}

// ===========================================================================
// FAKE IMPLEMENTATION  —  offline, deterministic, echoes like a real echo server
// ===========================================================================

/**
 * FakeWebSocketClient — emits a scripted Connecting -> Open (plus a greeting) and
 * echoes back whatever you [send], exactly like the real echo server — but with NO
 * network. This lets the app run offline and the unit tests assert behaviour
 * deterministically.
 */
class FakeWebSocketClient : LiveConnection {

    private val events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)

    override fun events(): Flow<ConnectionEvent> = events.asSharedFlow()

    override fun connect() {
        events.tryEmit(ConnectionEvent.Connecting)
        events.tryEmit(ConnectionEvent.Open)
        events.tryEmit(
            ConnectionEvent.MessageReceived("Connected to the OFFLINE fake echo server.")
        )
    }

    override fun send(text: String) {
        // A real echo server returns exactly what you send; the fake mirrors that so
        // the UI's "received" path is exercised without a network.
        events.tryEmit(ConnectionEvent.MessageReceived(text))
    }

    override fun close() {
        events.tryEmit(ConnectionEvent.Closed("client closed"))
    }
}

// ===========================================================================
// THE SWITCH  —  pick REAL (live wss://) or FAKE (offline) in one place
// ===========================================================================

/**
 * Factory that returns the connection the app should use.
 *
 * @param useFake when TRUE (the default), returns the offline [FakeWebSocketClient]
 *   so the project builds, runs, and tests with NO network. Flip it to FALSE to open
 *   a live WebSocket to wss://echo.websocket.events (requires INTERNET + a connection).
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │  STUDENTS: change this single argument to toggle between offline and live. │
 *   └──────────────────────────────────────────────────────────────────────────┘
 */
fun provideLiveConnection(useFake: Boolean = true): LiveConnection =
    if (useFake) FakeWebSocketClient()         // offline default: works with no internet
    else RealWebSocketClient()                 // live: requires INTERNET + a connection
