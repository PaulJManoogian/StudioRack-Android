package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceAttachmentSizingTest {
    @Test
    fun renderWidthScalesWithAvailableHeap() {
        assertEquals(1080, performanceAttachmentRenderWidth(192L * 1024 * 1024))
        assertEquals(1280, performanceAttachmentRenderWidth(320L * 1024 * 1024))
        assertEquals(1440, performanceAttachmentRenderWidth(512L * 1024 * 1024))
    }

    @Test
    fun cacheUsesBoundedShareOfHeap() {
        assertEquals(12 * 1024 * 1024, performanceAttachmentCacheBytes(96L * 1024 * 1024))
        assertEquals(24 * 1024 * 1024, performanceAttachmentCacheBytes(240L * 1024 * 1024))
        assertEquals(32 * 1024 * 1024, performanceAttachmentCacheBytes(512L * 1024 * 1024))
    }

    @Test
    fun imageSamplingKeepsLargeChartsNearDisplaySize() {
        assertEquals(4, performanceAttachmentSampleSize(6000, 8000, 1080, 2160))
        assertEquals(1, performanceAttachmentSampleSize(1000, 1600, 1080, 2160))
        assertEquals(1, performanceAttachmentSampleSize(0, 0, 1080, 2160))
    }
}
