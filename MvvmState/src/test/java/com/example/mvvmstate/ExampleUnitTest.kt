package com.example.mvvmstate                                  // same package as the code under test

import org.junit.Test                                          // marks a method as a JUnit test case
import org.junit.Assert.*                                      // assertEquals / assertTrue / assertNotEquals

/**
 * Example local unit test, which executes on the development machine (host JVM).
 *
 * The original sample test is kept (so the example still compiles & runs), plus a
 * couple of tiny assertions that reinforce THIS project's concept: state is
 * IMMUTABLE and only changes by producing a NEW value via copy().
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)                                 // the original smoke test
    }

    @Test
    fun copy_producesNewImmutableNote_withoutMutatingOriginal() {
        val original = Note(id = 1, title = "Learn StateFlow", done = false)
        val toggled = original.copy(done = true)               // immutable update -> a NEW Note

        assertFalse(original.done)                             // the original is untouched (immutability)
        assertTrue(toggled.done)                               // the copy carries the change
        assertNotEquals(original, toggled)                     // they are distinct values
    }

    @Test
    fun uiStateCopy_changesOneField_keepsTheRest() {
        val state = NotesUiState(newNoteTitle = "draft", isLoading = false)
        val updated = state.copy(newNoteTitle = "")            // clear only the draft via copy()

        assertEquals("", updated.newNoteTitle)                 // the targeted field changed...
        assertEquals(state.isLoading, updated.isLoading)       // ...and every other field is preserved
    }
}
