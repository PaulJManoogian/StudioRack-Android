package com.manoogianmedia.studiorack

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.SupportingRecord
import com.manoogianmedia.studiorack.data.CachedAttachment
import com.manoogianmedia.studiorack.data.StudioRackDatabase
import com.manoogianmedia.studiorack.data.SyncState
import com.manoogianmedia.studiorack.data.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import com.manoogianmedia.studiorack.performance.NativeMetronome
import com.manoogianmedia.studiorack.ui.mediaIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineSetListFixtureTest {
    @Test
    fun createsExternalViewIntentsForSongMedia() {
        val youtube = mediaIntent("https://www.youtube.com/watch?v=example")
        val spotify = mediaIntent("https://open.spotify.com/track/example")

        assertEquals(Intent.ACTION_VIEW, youtube?.action)
        assertEquals("https://www.youtube.com/watch?v=example", youtube?.dataString)
        assertEquals(Intent.ACTION_VIEW, spotify?.action)
        assertEquals("https://open.spotify.com/track/example", spotify?.dataString)
    }

    @Test
    fun seedsAnEditableOfflinePerformanceFixture() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = StudioRackDatabase.create(context)
        val dao = database.dao()
        dao.clearRecords()
        dao.clearSupporting()
        dao.putSupporting(listOf(SupportingRecord("item", "qa_cymbal", """{"id":"qa_cymbal","display_name":"16-inch Medium Thin Crash","quantity":1}""")))
        dao.putRecords(
            listOf(
                CachedRecord("maintenance_note", "qa_clean", 1, """{"id":"qa_clean","item_id":"qa_cymbal","status":"pending","note":"Clean the cymbal after the outdoor performance."}"""),
                CachedRecord("song", "song_fixture_1", 1, """{"id":"song_fixture_1","title":"Natural Apple Delight","artist":"Planet 10","style":"Rock","tempo":"120","time_signature":"4/4","starts_by":"Guitar","notes":"Arrangement by TTMB"}"""),
                CachedRecord("song", "song_fixture_2", 1, """{"id":"song_fixture_2","title":"Proud Mary","artist":"CCR","style":"Southern Rock","tempo":"120","time_signature":"4/4","starts_by":"All"}"""),
                CachedRecord("set_list", "setlist_fixture", 1, """{"id":"setlist_fixture","name":"Practice","description":"Offline editor QA","attachment_print_mode":"none","is_favorite":1}"""),
                CachedRecord("set_list_section", "sls_fixture", 1, """{"id":"sls_fixture","set_list_id":"setlist_fixture","name":"Part 1","position":0,"notes":""}"""),
                CachedRecord("set_list_entry", "sle_fixture", 1, """{"id":"sle_fixture","set_list_id":"setlist_fixture","section_id":"sls_fixture","song_id":"song_fixture_2","position":0,"manual_title":"","entry_notes":""}"""),
                CachedRecord("studio_event", "event_fixture", 1, """{"id":"event_fixture","event_type":"rehearsal","title":"Personal Rehearsal","event_date":"2026-09-12","start_time":"20:00","location":"Lansdale, PA","set_list_id":"setlist_fixture","event_status":"scheduled"}"""),
                CachedRecord("song_attachment", "attachment_fixture", 1, """{"id":"attachment_fixture","song_id":"song_fixture_2","attachment_type":"chart","display_name":"Proud Mary Chart","file_ref":"/studiorack/static/charts/proud-mary.png","is_gig_default":1,"include_in_print":1,"position":1}"""),
            )
        )
        val chartFile = File(context.filesDir, "fixture-chart.png")
        val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 5f; textSize = 72f }
        canvas.drawText("Proud Mary", 80f, 120f, paint)
        repeat(8) { row -> canvas.drawLine(80f, 260f + row * 140f, 1120f, 260f + row * 140f, paint) }
        FileOutputStream(chartFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        dao.putCachedAttachment(
            CachedAttachment(
                attachmentId = "attachment_fixture",
                songId = "song_fixture_2",
                revision = 1,
                fileRef = "/studiorack/static/charts/proud-mary.png",
                displayName = "Proud Mary Chart",
                attachmentType = "chart",
                localPath = chartFile.absolutePath,
                mimeType = "image/png",
                sha256 = null,
                byteCount = chartFile.length(),
                status = "ready",
            )
        )
        dao.putState(SyncState(accountJson = """{"studio_name":"Manoogian Media Studio","city":"Lansdale","state":"PA"}"""))
        TokenStore(context).save("visual-fixture-token", "visual-fixture-device", "visual-fixture-account")
        assertEquals(2, dao.records("song").size)
        assertEquals(1, dao.records("set_list_entry").size)
        database.close()
    }

    @Test
    fun metronomeMaintainsItsMonotonicBeatInterval() = runBlocking {
        val metronome = NativeMetronome()
        metronome.configure("120", "4/4", "downbeat", "tone")
        metronome.start()
        val timestamps = try {
            withTimeout(4_000) {
                metronome.state.filter { it.running && it.pulse }.map { System.nanoTime() }.take(6).toList()
            }
        } finally {
            metronome.close()
        }
        val intervals = timestamps.zipWithNext { first, second -> (second - first) / 1_000_000L }
        assertEquals(5, intervals.size)
        assertTrue("Beat intervals were $intervals", intervals.all { it in 440L..560L })
    }
}
