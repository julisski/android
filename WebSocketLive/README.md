# WebSocket Live

A single-screen Jetpack Compose app that opens a **WebSocket** connection, shows a
live **connection status**, streams a running **transcript**, and lets you send text
that the echo server sends straight back. It teaches a **second network protocol** —
full-duplex `ws://`/`wss://` — to contrast with the HTTP request/response of the
`NetworkParsing` demo, using **OkHttp's WebSocket** behind a `ViewModel` exposing a
`StateFlow`.

## Learning goal

Learn how an Android app does **backend communication over a different protocol** and
how to surface a live stream as UI state:

- **WebSocket vs HTTP** — HTTP is one request → one response; a WebSocket keeps a single
  connection open so either side can push messages at any time. The screen models that
  as a *stream* of events, not a single call.
- **Events as a Flow** — `OkHttp`'s `WebSocketListener` callbacks (`onOpen`, `onMessage`,
  `onClosing`, `onFailure`) are funnelled into a `Flow<ConnectionEvent>` the ViewModel
  collects.
- **A pure reducer** — `reduceLiveState(state, event)` folds each event into one
  immutable `LiveUiState` (status + transcript). Being pure, the whole state machine is
  unit-testable with no coroutines.
- **Optimistic UI** — `send()` appends your outgoing line immediately; the server's echo
  arrives later as a `MessageReceived` and is appended by the reducer.
- **Real vs Fake switch** — a `LiveConnection` interface with an OkHttp-backed `Real`
  impl and an in-memory `Fake` that scripts a connect and echoes sends, swapped by one
  flag so the app and tests run offline.

## Key files

- `src/main/java/com/example/websocketlive/ConnectionEvent.kt` — the sealed
  `ConnectionEvent` (wire events) plus the `ChatMessage` / `ConnectionStatus` UI models.
- `src/main/java/com/example/websocketlive/WebSocketClient.kt` — the `LiveConnection`
  interface plus `RealWebSocketClient` (OkHttp `WebSocket`) and `FakeWebSocketClient`
  (offline echo), and `provideLiveConnection(useFake = …)` — **the switch**.
- `src/main/java/com/example/websocketlive/LiveViewModel.kt` — `LiveUiState`, the pure
  `reduceLiveState`, and the ViewModel that collects the event stream into a `StateFlow`.
- `src/main/java/com/example/websocketlive/MainActivity.kt` — the Compose UI:
  `LiveScreen` (opens the connection, owns the input) and the stateless `LiveContent`
  that renders the status, transcript, and input row, plus `@Preview`s.
- `src/test/java/com/example/websocketlive/ExampleUnitTest.kt` — unit tests for the
  reducer, the fake connection's stream, and the ViewModel.

## What to inspect

- **`WebSocketClient.kt`**: how the `WebSocketListener` callbacks become
  `ConnectionEvent`s on a `MutableSharedFlow` (with `tryEmit`, so the OkHttp thread never
  suspends), and how the fake mirrors a real echo server with no network.
- **`LiveViewModel.kt`**: the pure `reduceLiveState` fold, and why `send()` appends the
  outgoing line itself while received frames flow in through `events()`.
- **`MainActivity.kt`**: `LaunchedEffect(Unit)` opens the socket exactly once;
  `collectAsStateWithLifecycle()` observes the state; the previews drive the **stateless**
  `LiveContent` with hand-made state — never a real socket/ViewModel.

## Run it

By default the app runs **offline** in fake mode (`provideLiveConnection(useFake = true)`),
so it builds and runs with **no network** and the unit tests need no internet. The fake
scripts a connect and echoes whatever you send, so the UI behaves just like the live
server.

To open the **live** WebSocket (`wss://echo.websocket.events`):

1. In `WebSocketClient.kt`, change the default to `provideLiveConnection(useFake = false)`.
2. Run on a device/emulator **with a network connection** (the `INTERNET` permission is
   already declared). Type a message — the echo server returns it on the open socket.

Build from the project root:

```bash
./gradlew assembleDebug --console=plain --stacktrace
./gradlew testDebugUnitTest
```
