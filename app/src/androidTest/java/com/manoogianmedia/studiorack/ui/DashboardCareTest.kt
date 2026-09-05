package com.manoogianmedia.studiorack.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DashboardCareTest {
    @Test
    fun maintenanceRowsUseHumanFacingItemDetailsAndCalculatedDates() {
        val item = JSONObject("""{"id":"itm_000048","display_name":"Piccolo Snare","brand_id":"brand_pearl","default_location_id":"studio","image_url":"/drumdb/static/images/snare.jpg"}""")
        val brand = JSONObject("""{"id":"brand_pearl","name":"Pearl"}""")
        val location = JSONObject("""{"id":"studio","name":"Main Studio"}""")
        val specs = listOf(
            JSONObject("""{"item_id":"itm_000048","key":"last_service_date","value":"2026-08-01"}"""),
            JSONObject("""{"item_id":"itm_000048","key":"service_interval_days","value":"30"}"""),
            JSONObject("""{"item_id":"itm_000048","key":"consumables_tracked","value":"Drum heads"}"""),
            JSONObject("""{"item_id":"itm_000048","key":"bot_notes","value":"Check the batter head."}"""),
        )

        val row = maintenanceRows(specs, listOf(item), listOf(brand), listOf(location), LocalDate.parse("2026-09-04")).single()

        assertEquals("Pearl", row.brand)
        assertEquals("Piccolo Snare", row.name)
        assertEquals("2026-08-31", row.dueDate)
        assertEquals("Past due", row.statusLabel)
        assertEquals("Drum heads", row.careItem)
        assertEquals("Main Studio", row.location)
        assertEquals("Check the batter head.", row.notes)
    }

    @Test
    fun fieldMaintenanceNotesAppearWithoutAServiceSchedule() {
        val item = JSONObject("""{"id":"itm_000003","display_name":"16 Med-Thin Crash","brand_id":"brand_zildjian"}""")
        val brand = JSONObject("""{"id":"brand_zildjian","name":"Zildjian"}""")
        val fieldNote = JSONObject("""{"id":"note_clean","item_id":"itm_000003","status":"pending","note":"Remind me this needs to be cleaned."}""")

        val row = maintenanceRows(emptyList(), listOf(item), listOf(brand), emptyList(), fieldNotes = listOf(fieldNote)).single()

        assertEquals("attention", row.status)
        assertEquals("Needs attention", row.statusLabel)
        assertEquals("Field note", row.careItem)
        assertEquals("Remind me this needs to be cleaned.", row.notes)
    }
}
