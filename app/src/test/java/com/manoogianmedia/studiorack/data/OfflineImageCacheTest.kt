package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineImageCacheTest {
    @Test
    fun resolvesServerAndExternalImageReferences() {
        assertEquals(
            "https://www.manoogianmedia.com/drumdb/static/images/item.png",
            resolvedAssetUrl("/drumdb/static/images/item.png"),
        )
        assertEquals("https://example.com/kit.jpg", resolvedAssetUrl("https://example.com/kit.jpg"))
        assertEquals(
            "https://www.manoogianmedia.com/studiorack/static/images/logo.png",
            resolvedAssetUrl("static/images/logo.png"),
        )
        assertNull(resolvedAssetUrl("  "))
    }
}
