// =============================================================================
// NoteApi.kt  —  RETROFIT: turning a Kotlin interface into HTTP calls
//
// CONCEPT THIS FILE TEACHES: with Retrofit you DESCRIBE the network endpoint as an
// annotated interface method; Retrofit generates the actual HTTP code at runtime.
// You never write sockets, URLs-by-hand, or manual JSON parsing — you declare
// "GET /posts returns a List<NoteDto>" and call a suspend function.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. @GET("posts") — the relative path appended to the Retrofit baseUrl.
//   2. `suspend fun` — the call is a coroutine; it suspends instead of blocking a thread.
//   3. The return type List<NoteDto> — Retrofit + the kotlinx.serialization converter
//      decode the JSON array straight into a list of @Serializable DTOs.
//   4. provideNoteApi() — how Retrofit is assembled: baseUrl + the JSON converter.
// =============================================================================

// Package declaration: ties this file to the app's namespace + directory layout.
package com.example.networkparsing

// Tells Retrofit/OkHttp which media type the response body is, when wiring the converter.
import okhttp3.MediaType.Companion.toMediaType
// The configurable JSON engine from kotlinx.serialization (we set ignoreUnknownKeys).
import kotlinx.serialization.json.Json
// Retrofit's builder — assembles a working client from baseUrl + converter.
import retrofit2.Retrofit
// The bridge that lets Retrofit decode bodies using kotlinx.serialization instead of Gson/Moshi.
import retrofit2.converter.kotlinx.serialization.asConverterFactory
// The HTTP-GET annotation: marks fetchNotes() as an HTTP GET request.
import retrofit2.http.GET

// ===========================================================================
// API SURFACE  —  the endpoint(s) this app talks to, described as an interface
// ===========================================================================

/**
 * NoteApi — the Retrofit description of the remote endpoints. Retrofit creates a
 * concrete implementation of this interface at runtime (see [provideNoteApi]).
 *
 * There is exactly one call here: fetch every post from `/posts`.
 */
interface NoteApi {

    /**
     * GET {baseUrl}/posts → a JSON array of post objects, decoded into [NoteDto]s.
     *
     * `suspend` means callers await it from a coroutine; Retrofit runs the network I/O
     * off the main thread and resumes you with the parsed list. No callbacks, no threads.
     */
    @GET("posts")                              // <-- THE concept: relative path -> HTTP GET
    suspend fun fetchNotes(): List<NoteDto>    // JSON array -> List<NoteDto>, parsed for you
}

// ===========================================================================
// RETROFIT ASSEMBLY  —  baseUrl + the kotlinx.serialization JSON converter
// ===========================================================================

// The configured JSON parser shared by the converter.
//   ignoreUnknownKeys = true  -> if the server adds NEW fields we didn't model, decoding
//   keeps working instead of throwing. This is the #1 real-world robustness setting.
private val json = Json { ignoreUnknownKeys = true }

/**
 * Build a ready-to-use [NoteApi] backed by Retrofit.
 *
 * Wiring, step by step:
 *   • baseUrl — the root every @GET path is appended to. MUST end in "/".
 *   • addConverterFactory — installs the kotlinx.serialization converter so response
 *     bodies are decoded by [json] into our @Serializable DTOs.
 *   • create(NoteApi::class.java) — Retrofit generates the implementation of NoteApi.
 */
fun provideNoteApi(): NoteApi =
    Retrofit.Builder()
        // The public, no-auth, no-key JSON test API. Trailing "/" is required by Retrofit.
        .baseUrl("https://jsonplaceholder.typicode.com/")
        // Plug kotlinx.serialization into Retrofit as the JSON (de)serializer.
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()                               // finalize the Retrofit instance
        .create(NoteApi::class.java)           // generate the NoteApi implementation
