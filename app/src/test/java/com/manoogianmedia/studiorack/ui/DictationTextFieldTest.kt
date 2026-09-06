package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationTextFieldTest {
    @Test
    fun replacesShortFieldText() {
        assertEquals("Zildjian crash", mergeDictation("old search", " Zildjian crash ", append = false))
    }

    @Test
    fun appendsLongFormDictation() {
        assertEquals(
            "Clean after the show. Replace the head next week.",
            mergeDictation("Clean after the show.", "Replace the head next week.", append = true),
        )
    }

    @Test
    fun ignoresEmptyRecognitionResult() {
        assertEquals("Keep this note", mergeDictation("Keep this note", "  ", append = true))
    }

    @Test
    fun usesNaturalRecognitionPrompts() {
        assertEquals("a search phrase", dictationPrompt("Find an item"))
        assertEquals("your notes", dictationPrompt("What needs attention?"))
        assertEquals("song title", dictationPrompt("Song title"))
    }
}
