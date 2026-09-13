package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpContentTest {
    @Test
    fun includesEveryWebHelpAreaAndOfflinePerformanceGuidance() {
        val sections = studioLeviathanHelpSections("Studio Leviathan", "Crew", "Leviathan Live")

        assertEquals(
            listOf("quick", "equipment", "music", "schedule", "people", "crew", "reports", "mobile", "settings", "troubleshooting"),
            sections.map { it.id },
        )

        val content = sections.flatMap { it.topics }.flatMap { it.paragraphs + it.points }.joinToString(" ")
        assertTrue(content.contains("Local Host"))
        assertTrue(content.contains("Bluetooth Page Turner"))
        assertTrue(content.contains("Performance Material"))
        assertTrue(content.contains("Guest Link"))
        assertTrue(content.contains("record_json"))
    }
}
