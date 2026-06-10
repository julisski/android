// =============================================================================
// ExampleUnitTest.kt  —  local JVM unit tests for the WebSocket state layer
//
// These run on the development machine (no device, no network). They verify the
// testable pieces of the concept:
//   1. reduceLiveState — the PURE event-folding function (no coroutines needed).
//   2. FakeWebSocketClient — connect() scripts Open + a greeting; send() echoes.
//   3. LiveViewModel — send() optimistically appends our outgoing line, and the
//      collected event stream folds a server message into the transcript.
//
// The fake-client and ViewModel tests use a test dispatcher that SHARES runTest's
// scheduler so collection + emission are advanced deterministically.
// =============================================================================
package com.example.websocketlive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit test suite for the WebSocketLive state layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)   // StandardTestDispatcher / advanceUntilIdle / runCurrent
class ExampleUnitTest {

    /** Sanity check kept from the template — confirms the test toolchain runs. */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    // --- Pure reducer (no coroutines) ----------------------------------------

    /** Open moves the status to Connected. */
    @Test
    fun reducer_open_setsConnected() {
        val next = reduceLiveState(LiveUiState(), ConnectionEvent.Open)
        assertEquals(ConnectionStatus.Connected, next.status)
    }

    /** A received message is appended as a NOT-from-me transcript line. */
    @Test
    fun reducer_messageReceived_appendsServerLine() {
        val next = reduceLiveState(LiveUiState(), ConnectionEvent.MessageReceived("hi"))
        assertEquals(1, next.messages.size)
        assertEquals("hi", next.messages.first().text)
        assertTrue("server message should not be fromMe", !next.messages.first().fromMe)
    }

    /** Failure flips the status to Failed. */
    @Test
    fun reducer_failure_setsFailed() {
        val next = reduceLiveState(LiveUiState(), ConnectionEvent.Failure("boom"))
        assertEquals(ConnectionStatus.Failed, next.status)
    }

    // --- Fake connection (stream behaviour) ----------------------------------

    /** connect() emits Open + a greeting; send() echoes the payload straight back. */
    @Test
    fun fakeClient_connectAndEcho() = runTest {
        val client = FakeWebSocketClient()
        val received = mutableListOf<ConnectionEvent>()
        // Collect in the background, then subscribe (runCurrent) BEFORE emitting so the
        // hot SharedFlow delivers to an active collector.
        val job = launch { client.events().collect { received += it } }
        runCurrent()

        client.connect()
        client.send("ping")
        runCurrent()
        job.cancel()

        assertTrue("should report Open", received.contains(ConnectionEvent.Open))
        assertTrue(
            "should echo the sent payload",
            received.any { it is ConnectionEvent.MessageReceived && it.text == "ping" },
        )
    }

    // --- ViewModel ------------------------------------------------------------

    /** send() optimistically appends our outgoing line and ignores blank input. */
    @Test
    fun viewModel_send_appendsOutgoingAndIgnoresBlank() = runTest {
        // The ViewModel launches its event collector in init, so constructing it needs
        // a Main dispatcher even though send()'s append is synchronous.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = LiveViewModel(FakeWebSocketClient())

            viewModel.send("   ")                            // blank -> ignored
            assertTrue(viewModel.uiState.value.messages.isEmpty())

            viewModel.send("hello")                          // real -> appended as fromMe
            val messages = viewModel.uiState.value.messages
            assertTrue(messages.any { it.fromMe && it.text == "hello" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** The ViewModel folds collected events: connect() ends in Connected + a server line. */
    @Test
    fun viewModel_collectsEventsIntoState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = LiveViewModel(FakeWebSocketClient())
            advanceUntilIdle()                               // let the init collector subscribe

            viewModel.connect()                              // now emits to an active collector
            advanceUntilIdle()                               // fold Connecting/Open/greeting

            val state = viewModel.uiState.value
            assertEquals(ConnectionStatus.Connected, state.status)
            assertTrue("should have a server greeting", state.messages.any { !it.fromMe })
        } finally {
            Dispatchers.resetMain()
        }
    }
}
