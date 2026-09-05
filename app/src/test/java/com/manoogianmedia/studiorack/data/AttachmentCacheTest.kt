package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentCacheTest {
    @Test
    fun preservesSupportedExtensionsWithoutQueryStrings() {
        assertEquals(".pdf", attachmentExtension("/charts/song.PDF?version=2"))
        assertEquals(".jpeg", attachmentExtension("photo.jpeg"))
        assertEquals(".bin", attachmentExtension("attachment"))
    }

    @Test
    fun infersDisplayMimeTypes() {
        assertEquals("application/pdf", attachmentMime("chart.pdf"))
        assertEquals("image/webp", attachmentMime("chart.webp"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            attachmentMime("notes.docx"),
        )
    }

    @Test
    fun keepsAuthenticatedDownloadsUnderCanonicalStudioRackPath() {
        assertEquals(
            "https://www.manoogianmedia.com/studiorack/api/v1/attachments/att_123",
            resolveDownloadUrl(
                "https://www.manoogianmedia.com/studiorack/api/v1",
                "/api/v1/attachments/att_123",
            ),
        )
    }
}
