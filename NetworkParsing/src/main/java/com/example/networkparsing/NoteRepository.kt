// =============================================================================
// NoteRepository.kt  —  REPOSITORY: one source of truth, REAL vs FAKE
//
// CONCEPT THIS FILE TEACHES: the rest of the app should not care WHERE notes come
// from. It asks a NoteRepository for notes; behind that interface sits either a REAL
// implementation (hits the network via Retrofit) or a FAKE one (returns hardcoded,
// already-parsed notes after a tiny delay). Swapping them is a ONE-LINE change, which
// is what lets the app + tests run completely OFFLINE.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. The NoteRepository interface — the single method getNotes(): List<Note>.
//   2. RealNoteRepository — calls the API, then maps each NoteDto -> Note.
//   3. FakeNoteRepository — no network; returns parsed-from-JSON notes after delay().
//   4. provideNoteRepository(useFake = ...) — THE SWITCH between online and offline.
// =============================================================================

// Package declaration: ties this file to the app's namespace + directory layout.
package com.example.networkparsing

// delay(): a non-blocking pause used by the fake to imitate network latency.
import kotlinx.coroutines.delay
// Decodes a hardcoded JSON string into List<NoteDto> so the fake exercises real parsing.
import kotlinx.serialization.json.Json
// The decodeFromString<T>() extension used to parse the hardcoded JSON below.
import kotlinx.serialization.decodeFromString

// ===========================================================================
// REPOSITORY CONTRACT  —  what callers (the ViewModel) depend on
// ===========================================================================

/**
 * NoteRepository — the abstraction the ViewModel talks to. It hides whether notes
 * arrive from the network or from memory.
 */
interface NoteRepository {
    /**
     * Return the list of [Note]s. `suspend` because the real implementation does
     * network I/O; the fake also suspends (via delay) so both share one signature.
     * May THROW on failure (e.g. no network) — the ViewModel catches that and turns
     * it into an Error UI state.
     */
    suspend fun getNotes(): List<Note>
}

// ===========================================================================
// REAL IMPLEMENTATION  —  network -> parse -> map
// ===========================================================================

/**
 * RealNoteRepository — fetches notes over HTTP using [NoteApi], then converts each
 * network [NoteDto] into a domain [Note]. Requires the INTERNET permission + a live
 * connection at runtime.
 *
 * @property api the Retrofit-backed API (defaults to a freshly built one).
 */
class RealNoteRepository(
    private val api: NoteApi = provideNoteApi(),
) : NoteRepository {

    override suspend fun getNotes(): List<Note> {
        // 1) NETWORK + PARSE: Retrofit performs the GET and decodes the JSON array
        //    into List<NoteDto> for us. If the network is down this line throws.
        val dtos: List<NoteDto> = api.fetchNotes()
        // 2) MAP: convert each network-shaped DTO into a clean domain Note.
        return dtos.map { it.toNote() }        // <-- DTO -> domain, the parsing payoff
    }
}

// ===========================================================================
// FAKE IMPLEMENTATION  —  offline, deterministic, but STILL parses JSON
// ===========================================================================

// A small hardcoded JSON payload shaped EXACTLY like the real `/posts` response.
// Using real JSON (instead of building NoteDto objects by hand) means the fake still
// demonstrates the decode-from-JSON step — it just skips the network.
private val FAKE_NOTES_JSON = """
    [
      { "userId": 1, "id": 1, "title": "Offline note one", "body": "This note was parsed from a hardcoded JSON string, no network required." },
      { "userId": 1, "id": 2, "title": "Offline note two", "body": "The FAKE repository lets the app and the unit tests run completely offline." },
      { "userId": 2, "id": 3, "title": "Offline note three", "body": "Flip useFake to false in provideNoteRepository() to hit the real API instead." }
    ]
""".trimIndent()

/**
 * FakeNoteRepository — returns the same parsed notes every time, after a short
 * [delay] to imitate a slow network so you can actually SEE the Loading state.
 * Never touches the network, so it works with the device in airplane mode and in
 * unit tests that have no internet.
 */
class FakeNoteRepository : NoteRepository {

    // Reuse the robust JSON config (ignore unknown keys) for the hardcoded payload.
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getNotes(): List<Note> {
        // Imitate network latency so the Loading spinner is visible for ~600ms.
        delay(600)
        // Parse the hardcoded JSON into DTOs (same path the real one uses), then map.
        val dtos: List<NoteDto> = json.decodeFromString(FAKE_NOTES_JSON)
        return dtos.map { it.toNote() }
    }
}

// ===========================================================================
// THE SWITCH  —  pick REAL (online) or FAKE (offline) in exactly one place
// ===========================================================================

/**
 * Factory that returns the repository the app should use.
 *
 * @param useFake when TRUE (the default here), returns the offline [FakeNoteRepository]
 *   so the project builds, runs, and tests with NO network. Flip it to FALSE to use
 *   [RealNoteRepository] and hit the live https://jsonplaceholder.typicode.com/posts.
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │  STUDENTS: change this single argument to toggle between offline and live. │
 *   └──────────────────────────────────────────────────────────────────────────┘
 */
fun provideNoteRepository(useFake: Boolean = true): NoteRepository =
    if (useFake) FakeNoteRepository()          // offline default: works with no internet
    else RealNoteRepository()                  // live: requires INTERNET + a connection
