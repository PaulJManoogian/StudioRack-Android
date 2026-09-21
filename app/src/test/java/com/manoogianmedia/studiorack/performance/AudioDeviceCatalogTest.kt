package com.manoogianmedia.studiorack.performance

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDeviceCatalogTest {
    @Test
    fun choosesLargestCompatibleProfileForInterface() {
        val profiles = listOf(
            JSONObject().put("id", "stereo").put("name", "Stereo").put("output_channel_count", 2),
            JSONObject().put("id", "eight").put("name", "Eight outputs").put("output_channel_count", 8),
            JSONObject().put("id", "eighteen").put("name", "Eighteen outputs").put("output_channel_count", 18),
        )

        val result = AudioDeviceCatalog.compatibleProfileForChannels(10, profiles)!!

        assertEquals("eight", result.id)
        assertEquals(8, result.outputChannelCount)
        assertFalse(result.exactMatch)
    }

    @Test
    fun exactChannelProfileWins() {
        val profiles = listOf(
            JSONObject().put("id", "stereo").put("output_channel_count", 2).put("is_default", true),
            JSONObject().put("id", "eighteen").put("output_channel_count", 18),
        )

        val result = AudioDeviceCatalog.compatibleProfileForChannels(18, profiles)!!

        assertEquals("eighteen", result.id)
        assertTrue(result.exactMatch)
    }
}
