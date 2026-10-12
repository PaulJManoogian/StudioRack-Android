package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.nio.file.Files

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

    @Test
    fun refreshesOnePageMusicXmlWhenRenderedPageHasNotBeenCached() {
        val directory = Files.createTempDirectory("musicxml-cache").toFile()
        val source = directory.resolve("score.mxl").apply { writeText("source") }
        val current = cachedAttachment(localPath = source.absolutePath)

        assertFalse(musicXmlDerivativeCacheReady("score", readyNotation(1), current, directory))
    }

    @Test
    fun refreshesMusicXmlWhenAuditionFileIsMissing() {
        val directory = Files.createTempDirectory("musicxml-cache").toFile()
        val pageOne = directory.resolve("score-page-1.svg").apply { writeText("<svg/>") }
        val current = cachedAttachment(localPath = pageOne.absolutePath)

        assertFalse(musicXmlDerivativeCacheReady("score", readyNotation(1), current, directory))
    }

    @Test
    fun acceptsCompleteMultiPageMusicXmlCache() {
        val directory = Files.createTempDirectory("musicxml-cache").toFile()
        val pageOne = directory.resolve("score-page-1.svg").apply { writeText("<svg/>") }
        directory.resolve("score-page-2.svg").writeText("<svg/>")
        directory.resolve("score-audition.mid").writeText("midi")
        val current = cachedAttachment(localPath = pageOne.absolutePath)

        assertTrue(musicXmlDerivativeCacheReady("score", readyNotation(2), current, directory))
    }

    @Test
    fun ignoresDerivativeFilesForOrdinaryAttachments() {
        val directory = Files.createTempDirectory("attachment-cache").toFile()

        assertTrue(musicXmlDerivativeCacheReady("chart", JSONObject(), null, directory))
    }

    @Test
    fun restoresSideloadedDatabaseOnlyWhenReadyMarkerExists() {
        val root = Files.createTempDirectory("database-restore").toFile()
        val databases = root.resolve("databases")
        val migration = root.resolve("migration").apply { mkdirs() }
        migration.resolve("studiorack-offline.db").writeText("database")

        assertFalse(restoreSideloadedDatabase(databases, migration))
        migration.resolve("restore-ready").writeText("")
        assertTrue(restoreSideloadedDatabase(databases, migration))
        assertEquals("database", databases.resolve("studiorack-offline.db").readText())
        assertFalse(migration.exists())
    }

    private fun readyNotation(pageCount: Int) = JSONObject()
        .put("notation_status", "ready")
        .put("notation_page_count", pageCount)
        .put("audition_path", "/api/v1/attachments/score/audition")

    private fun cachedAttachment(localPath: String) = CachedAttachment(
        attachmentId = "score",
        songId = "song",
        revision = 1,
        fileRef = "score.mxl",
        displayName = "Score",
        attachmentType = "musicxml",
        localPath = localPath,
        mimeType = "image/svg+xml",
        sha256 = "hash",
        byteCount = 1,
        status = "ready",
        error = null,
        cachedAt = 1,
    )
}
