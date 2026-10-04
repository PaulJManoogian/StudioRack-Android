package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarToolsTest {
    @Test
    fun monthCanStartOnSundayOrMondayAndIncludesMultiDayEvents() {
        val event = JSONObject()
            .put("title", "Tour Run")
            .put("event_date", "2026-10-02")
            .put("end_date", "2026-10-04")

        val sunday = buildCalendarMonth(YearMonth.of(2026, 10), listOf(event), sundayFirst = true, today = LocalDate.of(2026, 10, 4))
        val monday = buildCalendarMonth(YearMonth.of(2026, 10), listOf(event), sundayFirst = false, today = LocalDate.of(2026, 10, 4))

        assertEquals(LocalDate.of(2026, 9, 27), sunday.first().date)
        assertEquals(LocalDate.of(2026, 9, 28), monday.first().date)
        assertTrue(sunday.first { it.date == LocalDate.of(2026, 10, 3) }.events.isNotEmpty())
        assertTrue(sunday.first { it.date == LocalDate.of(2026, 10, 4) }.isToday)
    }

    @Test
    fun eventWindowRejectsBackwardsDatesAndTimes() {
        assertEquals("The end date cannot be before the start date.", validateEventWindow("2026-10-04", "", "2026-10-03", "", true))
        assertEquals("The end time must be after the start time.", validateEventWindow("2026-10-04", "20:00", "2026-10-04", "19:00", false))
        assertNull(validateEventWindow("2026-10-04", "20:00", "2026-10-05", "01:00", false))
    }

    @Test
    fun calendarFileCarriesPrivacyImportanceAndInclusiveAllDayRange() {
        val event = JSONObject()
            .put("id", "event_1")
            .put("title", "Private Festival")
            .put("event_date", "2026-10-02")
            .put("end_date", "2026-10-04")
            .put("all_day", 1)
            .put("is_private", 1)
            .put("importance", "critical")
            .put("notes", "Bring charts")

        val ics = buildEventIcs(event, "Main Stage, Newtown", ZoneId.of("America/New_York"))

        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261002"))
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261005"))
        assertTrue(ics.contains("CLASS:PRIVATE"))
        assertTrue(ics.contains("PRIORITY:1"))
        assertTrue(ics.contains("LOCATION:Main Stage\\, Newtown"))
        assertFalse(ics.contains("microsoft_graph"))
    }
}
