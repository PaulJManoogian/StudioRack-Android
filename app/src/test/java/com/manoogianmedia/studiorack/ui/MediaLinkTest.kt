package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLinkTest {
    @Test
    fun acceptsSupportedMediaLinks() {
        assertEquals("https://open.spotify.com/track/example", normalizedMediaLink(" https://open.spotify.com/track/example "))
        assertEquals("https://www.youtube.com/watch?v=example", normalizedMediaLink("https://www.youtube.com/watch?v=example"))
        assertEquals("spotify:track:example", normalizedMediaLink("spotify:track:example"))
    }

    @Test
    fun rejectsBlankOrUnsafeLinks() {
        assertNull(normalizedMediaLink(""))
        assertNull(normalizedMediaLink("javascript:alert(1)"))
        assertNull(normalizedMediaLink("not a link"))
    }
}
