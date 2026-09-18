package com.manoogianmedia.studiorack.crew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrewBehaviorSettingsTest {
    @Test
    fun jsonRoundTripPreservesBehaviorChoices() {
        val original = CrewBehaviorSettings(
            persistenceLevel = 5,
            adaptiveTiming = true,
            warmthLevel = 4,
            responseDetail = "brief",
            humorEnabled = true,
            proactiveSuggestions = false,
            uncertaintyStyle = "ask_first",
            preferredChannels = listOf("mobile", "email"),
            phrasesToAvoid = "You failed",
            communicationNotes = "Call rehearsals sessions.",
            approvedExamples = "I noticed this is due Saturday.",
        )

        val restored = CrewBehaviorSettings.fromJson(original.toJson(pendingSync = true).toString())

        assertEquals(original, restored)
        assertTrue(original.toJson(pendingSync = true).optInt("_mobile_pending") == 1)
    }
}
