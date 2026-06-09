// =============================================================================
// Note.kt  —  DTO  vs  DOMAIN MODEL  (the parsing boundary)
//
// CONCEPT THIS FILE TEACHES: keep the shape the *network* speaks (the DTO) SEPARATE
// from the shape your *app* speaks (the domain model), and convert between them in
// ONE place (a mapper function). This is the heart of "JSON parsing": JSON text
// arrives, kotlinx.serialization turns it into a NoteDto, and a mapper turns that
// into a clean Note the rest of the app uses.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. @Serializable on NoteDto — this is what makes the JSON-decoding possible.
//   2. The field names on NoteDto MATCH the JSON keys exactly (userId/id/title/body).
//   3. Note (the domain model) is a PLAIN data class — NO serialization annotations,
//      and it DROPS the fields the UI doesn't care about (userId).
//   4. NoteDto.toNote() — the single mapping point from "network shape" to "app shape".
// =============================================================================

// Package declaration: ties this file to the app's namespace + directory layout.
package com.example.networkparsing

// kotlinx.serialization marker: tells the serialization compiler plugin to GENERATE
// a serializer for this class so JSON text can be decoded into it (and back).
import kotlinx.serialization.Serializable

// ===========================================================================
// DATA TRANSFER OBJECT (DTO)  —  the exact shape of one JSON object from the API
//
// Sample JSON from https://jsonplaceholder.typicode.com/posts :
//   { "userId": 1, "id": 1, "title": "sunt aut...", "body": "quia et..." }
//
// Every property below mirrors a JSON key by NAME and TYPE. If the JSON key and
// the property name ever differ, you'd add @SerialName("json_key") — but here they
// already match, so no extra annotations are needed.
// ===========================================================================

/**
 * NoteDto — the raw, on-the-wire representation of a single post as returned by the
 * `/posts` endpoint. It exists ONLY to be (de)serialized; it is the "network shape".
 *
 * @property userId who authored the post. We parse it (so decoding doesn't fail) but
 *   the UI does not need it — note that it is intentionally NOT carried into [Note].
 * @property id the post's unique id from the server.
 * @property title the post headline.
 * @property body the post's longer text.
 */
@Serializable                                  // <-- THE concept: makes this class JSON-decodable
data class NoteDto(
    val userId: Int,                           // matches JSON "userId" (parsed, then dropped)
    val id: Int,                               // matches JSON "id"
    val title: String,                         // matches JSON "title"
    val body: String,                          // matches JSON "body"
)

// ===========================================================================
// DOMAIN MODEL  —  the clean shape the rest of the app actually uses
//
// This is a PLAIN data class: no @Serializable, no knowledge of JSON, no `userId`.
// Decoupling it from NoteDto means the UI/ViewModel never depend on the API's exact
// wire format — if the API renamed a field or added 20 more, only NoteDto + the
// mapper below would change, not the screens or ViewModel.
// ===========================================================================

/**
 * Note — the app's own representation of a note. Built FROM a [NoteDto] by [toNote].
 *
 * @property id the note's id (carried straight through from the DTO).
 * @property title the headline shown in the list.
 * @property body the body text shown under the title.
 */
data class Note(
    val id: Int,                               // kept from the DTO
    val title: String,                         // kept from the DTO
    val body: String,                          // kept from the DTO
    // NOTE: there is deliberately NO `userId` here — the UI doesn't need it, so the
    // domain model leaves it out. That is the whole point of a separate domain model.
)

// ===========================================================================
// MAPPER  —  the SINGLE place network-shape becomes app-shape
// ===========================================================================

/**
 * Convert one network [NoteDto] into a clean domain [Note].
 *
 * This is the only function that "knows" how the two shapes line up. Centralizing it
 * here means parsing logic lives in exactly one testable spot.
 */
fun NoteDto.toNote(): Note =                   // extension fn: reads like dto.toNote()
    Note(
        id = id,                               // copy id across
        title = title,                         // copy title across
        body = body,                           // copy body across
        // userId is intentionally ignored — it never crosses into the domain model.
    )
