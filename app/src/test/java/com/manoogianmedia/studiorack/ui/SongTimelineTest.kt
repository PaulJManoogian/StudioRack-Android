package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongTimelineTest {
    @Test
    fun synchronizedLyricsSupportMultipleTimestampsAndMetadata() {
        val lines = parseLrcTimeline("[ar:Rush]\n[00:01.25][00:03.500]Begin\n[01:02]Next")

        assertEquals(listOf(1_250L, 3_500L, 62_000L), lines.map(TimedLyricLine::atMs))
        assertEquals(listOf("Begin", "Begin", "Next"), lines.map(TimedLyricLine::text))
        assertEquals(1, activeLyricIndex(lines, 4_000L))
    }

    @Test
    fun songSectionsRespectOptionalEndTime() {
        val sections = listOf(
            TimedSongSection(0, 20_000, "Intro"),
            TimedSongSection(20_000, null, "Verse"),
        )

        assertEquals("Intro", activeSongSection(sections, 19_999)?.name)
        assertEquals("Verse", activeSongSection(sections, 20_000)?.name)
        assertNull(activeSongSection(listOf(TimedSongSection(0, 10_000, "Count In")), 10_000))
    }
}
