package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineImageCacheTest {
    private val publicBaseUrl = "https://www.manoogianmedia.com/leviathan"

    @Test
    fun resolvesServerAndExternalImageReferences() {
        assertEquals(
            "https://www.manoogianmedia.com/drumdb/static/images/item.png",
            resolvedAssetUrl("/drumdb/static/images/item.png", publicBaseUrl),
        )
        assertEquals("https://example.com/kit.jpg", resolvedAssetUrl("https://example.com/kit.jpg", publicBaseUrl))
        assertEquals(
            "https://www.manoogianmedia.com/leviathan/static/images/logo.png",
            resolvedAssetUrl("static/images/logo.png", publicBaseUrl),
        )
        assertNull(resolvedAssetUrl("  ", publicBaseUrl))
    }
}
