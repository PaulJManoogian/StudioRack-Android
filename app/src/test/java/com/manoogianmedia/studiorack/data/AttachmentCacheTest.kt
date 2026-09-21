package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AttachmentCacheTest {
    @Test
    fun preservesSupportedExtensionsWithoutQueryStrings() {
        assertEquals(".pdf", attachmentExtension("/charts/song.PDF?version=2"))
        assertEquals(".jpeg", attachmentExtension("photo.jpeg"))
        assertEquals(".mid", attachmentExtension("lighting.mid"))
        assertEquals(".bin", attachmentExtension("attachment"))
    }

    @Test
    fun infersDisplayMimeTypes() {
        assertEquals("application/pdf", attachmentMime("chart.pdf"))
        assertEquals("image/webp", attachmentMime("chart.webp"))
        assertEquals("audio/midi", attachmentMime("show-control.midi"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            attachmentMime("notes.docx"),
        )
    }

    @Test
    fun keepsAuthenticatedDownloadsUnderCanonicalProductPath() {
        assertEquals(
            "https://www.manoogianmedia.com/leviathan/api/v1/attachments/att_123",
            resolveDownloadUrl(
                "https://www.manoogianmedia.com/leviathan/api/v1",
                "/api/v1/attachments/att_123",
            ),
        )
    }

    @Test
    fun rejectsAuthenticatedDownloadsToAnotherHost() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveDownloadUrl(
                "https://www.manoogianmedia.com/leviathan/api/v1",
                "https://attacker.example/collect-token",
            )
        }
    }
}
