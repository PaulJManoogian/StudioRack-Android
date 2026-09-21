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

    @Test
    fun chordProSectionsUseLeviathanTimingWithoutChangingPortableSections() {
        val content = """{x_leviathan_time: 0:00-0:12}
{start_of_intro}
Count in
{end_of_intro}
{x_leviathan_time: 0:12}
{start_of_verse: label="Verse 1"}
Line one
{end_of_verse}
{start_of_verse}
Untimed line
{end_of_verse}"""

        val sections = parseChordProTimeline(content, 90_000)

        assertEquals(listOf("Intro", "Verse 1"), sections.map(TimedSongSection::name))
        assertEquals(listOf(0L, 12_000L), sections.map(TimedSongSection::atMs))
        assertEquals(listOf(12_000L, 90_000L), sections.map(TimedSongSection::endMs))
    }
}
