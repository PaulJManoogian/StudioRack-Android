package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class StudioRackNotificationsTest {
    @Test
    fun calculatesReminderLeadForSupportedUnits() {
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 18, 30),
            calculateEventReminderAt("2026-09-12", "20:30", 2, "hours"),
        )
        assertEquals(
            LocalDateTime.of(2026, 9, 10, 20, 30),
            calculateEventReminderAt("2026-09-12", "20:30", 2, "days"),
        )
        assertEquals(
            LocalDateTime.of(2026, 8, 29, 20, 30),
            calculateEventReminderAt("2026-09-12", "20:30", 2, "weeks"),
        )
    }

    @Test
    fun defaultsMissingTimeAndRejectsInvalidDates() {
        assertEquals(
            LocalDateTime.of(2026, 9, 11, 9, 0),
            calculateEventReminderAt("2026-09-12", "", 1, "days"),
        )
        assertNull(calculateEventReminderAt("not-a-date", "20:30", 1, "days"))
    }
}
