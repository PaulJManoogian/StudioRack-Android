package com.manoogianmedia.studiorack.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceToneDeviceTest {
    @After
    fun stopTone() {
        TunerReferenceTone.stop()
    }

    @Test
    fun mediaReferenceToneStartsOnDevice() {
        assertTrue(TunerReferenceTone.play(440.0, durationMs = 600))
        Thread.sleep(750)
    }
}
