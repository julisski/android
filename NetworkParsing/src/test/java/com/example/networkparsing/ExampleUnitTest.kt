// =============================================================================
// ExampleUnitTest.kt  —  local JVM unit tests for the parsing + offline data layer
//
// These run on the development machine (no device, no network). They verify the two
// pure, testable pieces of the concept:
//   1. NoteDto.toNote() correctly maps the network shape to the domain shape.
//   2. The offline FakeNoteRepository parses its hardcoded JSON into real Notes.
// runTest { } (from kotlinx-coroutines-test) lets us call the suspend getNotes().
// =============================================================================

package com.example.networkparsing

import org.junit.Test                                       // marks a method as a test case
import org.junit.Assert.*                                   // assertEquals / assertTrue / ...
import kotlinx.coroutines.test.runTest                      // runs suspend code in a test

/**
 * Local unit test suite for the NetworkParsing data layer.
 */
class ExampleUnitTest {

    /** Sanity check kept from the template — confirms the test toolchain compiles/runs. */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    /** The DTO -> domain mapper copies the kept fields and drops userId. */
    @Test
    fun noteDto_toNote_mapsFields() {
        val dto = NoteDto(userId = 7, id = 42, title = "Hello", body = "World")
        val note = dto.toNote()
        assertEquals(42, note.id)
        assertEquals("Hello", note.title)
        assertEquals("World", note.body)
        // (Note has no userId field at all — that's the decoupling we want.)
    }

    /** The offline fake parses its hardcoded JSON into a non-empty list of Notes. */
    @Test
    fun fakeRepository_returnsParsedNotes() = runTest {
        val repo = FakeNoteRepository()
        val notes = repo.getNotes()
        assertTrue("fake repo should return notes", notes.isNotEmpty())
        assertEquals("Offline note one", notes.first().title)
    }

    /** provideNoteRepository(useFake = true) yields the offline implementation. */
    @Test
    fun provideRepository_defaultsToFake() {
        assertTrue(provideNoteRepository(useFake = true) is FakeNoteRepository)
        assertTrue(provideNoteRepository(useFake = false) is RealNoteRepository)
    }
}
