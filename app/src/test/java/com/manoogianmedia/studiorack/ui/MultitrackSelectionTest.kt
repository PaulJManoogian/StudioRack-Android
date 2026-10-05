package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MultitrackSelectionTest {
    @Test
    fun `server main mix replaces synthetic fallback bus`() {
        val buses = listOf(
            JSONObject().put("id", "bus-main").put("name", "Main Mix"),
            JSONObject().put("id", "bus-click").put("name", "Click"),
        )

        assertEquals("bus-main", defaultMixerBusId(buses))
        assertEquals("__main__", defaultMixerBusId(emptyList()))
    }

    @Test
    fun singleArrangementBecomesDefaultWhenEntryHasNoPlaybackChoice() {
        val arrangement = JSONObject().put("id", "arr_1").put("song_id", "song_1")
        val entry = JSONObject().put("song_id", "song_1")

        val selected = selectPlaybackArrangement(
            entry,
            mapOf("arr_1" to arrangement),
            mapOf("song_1" to listOf(arrangement)),
        )

        assertSame(arrangement, selected)
    }

    @Test
    fun databaseNullPlaybackChoicesDoNotBlockTheArrangement() {
        val arrangement = JSONObject().put("id", "arr_1").put("song_id", "song_1")
        val entry = JSONObject()
            .put("song_id", "song_1")
            .put("playback_attachment_id", JSONObject.NULL)
            .put("playback_arrangement_id", JSONObject.NULL)

        val selected = selectPlaybackArrangement(
            entry,
            mapOf("arr_1" to arrangement),
            mapOf("song_1" to listOf(arrangement)),
        )

        assertSame(arrangement, selected)
    }

    @Test
    fun explicitAttachmentPreventsImplicitArrangementSelection() {
        val arrangement = JSONObject().put("id", "arr_1").put("song_id", "song_1")
        val entry = JSONObject().put("song_id", "song_1").put("playback_attachment_id", "stem_1")

        val selected = selectPlaybackArrangement(
            entry,
            mapOf("arr_1" to arrangement),
            mapOf("song_1" to listOf(arrangement)),
        )

        assertNull(selected)
    }

    @Test
    fun consistentlyTaggedTracksRecoverMissingArrangementMetadata() {
        val entry = JSONObject().put("song_id", "song_1")
        val tracks = listOf(
            JSONObject().put("id", "stem_1").put("audio_arrangement_id", "arr_1"),
            JSONObject().put("id", "stem_2").put("audio_arrangement_id", "arr_1"),
        )

        val selected = selectPlaybackArrangement(entry, emptyMap(), emptyMap(), tracks)

        assertEquals("arr_1", selected?.optString("id"))
        assertEquals("Multitrack Arrangement", selected?.optString("name"))
    }

    @Test
    fun ambiguousTrackGroupsDoNotSelectAnArrangement() {
        val entry = JSONObject().put("song_id", "song_1")
        val tracks = listOf(
            JSONObject().put("id", "stem_1").put("audio_arrangement_id", "arr_1"),
            JSONObject().put("id", "stem_2").put("audio_arrangement_id", "arr_2"),
        )

        assertNull(selectPlaybackArrangement(entry, emptyMap(), emptyMap(), tracks))
    }
}
