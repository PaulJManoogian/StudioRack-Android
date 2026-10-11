package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrumChartTest {
    @Test
    fun parsesNashvilleSectionsPatternsAndInstructions() {
        val document = parseDrumChart(
            """{title: Test Song}
                |{tempo: 120}
                |{time: 4/4}
                |[Intro] x4
                |pattern: HH|x-x-x-x-| SD|--o---o-| BD|o---o---|
                |cue: Guitar starts
                |[Chorus] x8
                |pattern: RD|x-x-x-x-| SD|--o---o-| BD|o-o---o-|
                |fill: last bar
            """.trimMargin()
        )

        assertEquals("120", document.metadata["tempo"])
        assertEquals(listOf("Intro", "Chorus"), document.sections.map { it.name })
        assertEquals(listOf(4, 8), document.sections.map { it.bars })
        assertEquals(listOf("HH", "SD", "BD"), document.sections.first().rows.map { it.label })
        assertTrue(document.sections.first().notes.first().startsWith("Cue:"))
    }

    @Test
    fun parsesStandaloneDrumTabRows() {
        val document = parseDrumChart("HH|x-x-x-x-|\nSD|--o---o-|\nBD|o---o---|")
        assertEquals(3, document.rows.size)
        assertEquals("o---o---", document.rows.last().pattern)
    }

    @Test
    fun parsesNotationInstructionsWithoutChangingPatternRows() {
        val document = parseDrumChart("""{time: 4/4}
            |[Chorus] x8
            |pattern: CR|>-------| HH|x-x-x-x-| SD|--g---f-| BD|o-o---o-|
            |sticking: R L R L
            |dynamics: mf
        """.trimMargin())
        assertEquals(listOf("CR", "HH", "SD", "BD"), document.sections.single().rows.map { it.label })
        assertTrue(document.sections.single().notes.contains("Sticking: R L R L"))
        assertTrue(document.sections.single().notes.contains("Dynamics: mf"))
    }
}

