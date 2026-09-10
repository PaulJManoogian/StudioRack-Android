package com.manoogianmedia.studiorack.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LocalExchangeExporterTest {
    private val exporter = LocalExchangeExporter(
        dao = errorDao(),
        productName = "Studio Leviathan",
        publisherName = "Manoogian Media",
        filePrefix = "studio-leviathan",
    )

    @Test
    fun `every format is generated locally with current branding`() {
        val store = LocalExportStore(
            entities = mapOf("song" to listOf(song("song_1", "Offline Song"))),
            supporting = emptyMap(),
        )

        listOf("csv", "xls", "json", "xml").forEach { format ->
            val result = exporter.export("songs", format, listOf("song_1"), store, LocalDate.of(2026, 9, 8))
            val text = result.bytes.toString(Charsets.UTF_8)

            assertEquals("studio-leviathan-songs-2026-09-08.$format", result.filename)
            assertTrue(text.contains("Offline Song"))
            assertTrue(text.contains("Bb"))
            assertFalse(text.contains("StudioRack", ignoreCase = true))
            if (format != "csv") assertTrue(text.contains("Studio Leviathan"))
        }
    }

    @Test
    fun `selected set list export includes ordered sets and song details`() {
        val store = LocalExportStore(
            entities = mapOf(
                "song" to listOf(song("song_1", "First Song"), song("song_2", "Second Song")),
                "set_list" to listOf(
                    JSONObject().put("id", "set_a").put("name", "First Set List"),
                    JSONObject().put("id", "set_b").put("name", "Selected Set List").put("description", "Road show").put("notes", "Set notes"),
                ),
                "set_list_section" to listOf(
                    JSONObject().put("id", "section_2").put("set_list_id", "set_b").put("name", "Encore").put("position", 2),
                    JSONObject().put("id", "section_1").put("set_list_id", "set_b").put("name", "Set One").put("position", 1),
                ),
                "set_list_entry" to listOf(
                    JSONObject().put("id", "entry_2").put("set_list_id", "set_b").put("section_id", "section_2").put("song_id", "song_2").put("position", 1),
                    JSONObject().put("id", "entry_1").put("set_list_id", "set_b").put("section_id", "section_1").put("song_id", "song_1").put("position", 1).put("entry_notes", "Count four"),
                ),
            ),
            supporting = emptyMap(),
        )

        val csv = exporter.export("setlists", "csv", listOf("set_b"), store, LocalDate.of(2026, 9, 8))
            .bytes.toString(Charsets.UTF_8)
        assertFalse(csv.contains("First Set List"))
        assertTrue(csv.indexOf("First Song") < csv.indexOf("Second Song"))
        assertTrue(csv.contains("Selected Set List,Road show,Set One,1,1,First Song,Test Artist,Bb,120,3:45,4/4,Rock,Guitar"))

        val json = JSONObject(exporter.export("setlists", "json", listOf("set_b"), store).bytes.toString(Charsets.UTF_8))
        val exported = json.getJSONArray("records").getJSONObject(0)
        assertEquals(1, json.getJSONArray("records").length())
        assertEquals("Set One", exported.getJSONArray("sections").getJSONObject(0).getString("name"))
        assertEquals("First Song", exported.getJSONArray("sections").getJSONObject(0).getJSONArray("entries").getJSONObject(0).getString("song_title"))
        assertEquals("Bb", exported.getJSONArray("sections").getJSONObject(0).getJSONArray("entries").getJSONObject(0).getString("song_key"))
    }

    @Test
    fun `equipment kits and schedule use cached relationship names`() {
        val store = LocalExportStore(
            entities = mapOf(
                "set_list" to listOf(JSONObject().put("id", "set_1").put("name", "Friday Show")),
                "studio_event" to listOf(JSONObject().put("id", "event_1").put("title", "Venue Date").put("set_list_id", "set_1")),
                "studio_event_kit" to listOf(JSONObject().put("event_id", "event_1").put("kit_id", "kit_1")),
            ),
            supporting = mapOf(
                "item" to listOf(JSONObject().put("id", "item_1").put("display_name", "Road Snare").put("brand_id", "brand_1").put("category_id", "category_1").put("type_id", "type_1")),
                "kit" to listOf(JSONObject().put("id", "kit_1").put("name", "Road Kit")),
                "brand" to listOf(JSONObject().put("id", "brand_1").put("name", "Example Brand")),
                "category" to listOf(JSONObject().put("id", "category_1").put("name", "Drums")),
                "item_type" to listOf(JSONObject().put("id", "type_1").put("name", "Snare Drum")),
            ),
        )

        val itemCsv = exporter.export("items", "csv", listOf("item_1"), store).bytes.toString(Charsets.UTF_8)
        assertTrue(itemCsv.contains("Road Snare,Example Brand,Drums,Snare Drum"))
        val eventCsv = exporter.export("events", "csv", listOf("event_1"), store).bytes.toString(Charsets.UTF_8)
        assertTrue(eventCsv.contains("Venue Date"))
        assertTrue(eventCsv.contains("Friday Show,Road Kit"))
    }

    private fun song(id: String, title: String) = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("artist", "Test Artist")
        .put("tempo", 120)
        .put("duration_seconds", 225)
        .put("time_signature", "4/4")
        .put("song_key", "Bb")
        .put("style", "Rock")
        .put("starts_by", "Guitar")

    private fun errorDao(): StudioRackDao = java.lang.reflect.Proxy.newProxyInstance(
        StudioRackDao::class.java.classLoader,
        arrayOf(StudioRackDao::class.java),
    ) { _, method, _ -> error("DAO method ${method.name} should not be called by serializer tests") } as StudioRackDao
}
