package com.manoogianmedia.studiorack.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrumChartDeviceTest {
    @Test
    fun parsesMetadataAndSectionsWithAndroidRegexEngine() {
        val document = parseDrumChart(
            """{title: Device Test}
                |{time: 4/4}
                |[Intro] x4
                |pattern: HH|x-x-x-x-| SD|--o---o-| BD|o---o---|
            """.trimMargin()
        )

        assertEquals("Device Test", document.metadata["title"])
        assertEquals("Intro", document.sections.single().name)
        assertEquals(4, document.sections.single().bars)
        assertTrue(document.sections.single().rows.any { it.label == "SD" })
    }
}
