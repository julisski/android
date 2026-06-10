// =============================================================================
// LocationData.kt  —  the DOMAIN MODEL for a single location fix
//
// CONCEPT THIS FILE TEACHES: just as the networking demo separated the "wire
// shape" (a JSON DTO) from the "app shape" (a domain model), here we keep the
// Android framework's android.location.Location OUT of the rest of the app. The
// repository converts a framework Location into this small, plain LocationData;
// the ViewModel and UI only ever see LocationData. That keeps the UI free of any
// dependency on Google Play services types and makes it trivial to fake in tests.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. LocationData is a PLAIN data class — no Android imports at all.
//   2. It carries only what the screen needs: latitude + longitude.
// =============================================================================
package com.example.locationservices

/**
 * LocationData — the app's own representation of one location fix.
 *
 * @property latitude  degrees north (+) / south (−) of the equator.
 * @property longitude degrees east (+) / west (−) of the prime meridian.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
)
