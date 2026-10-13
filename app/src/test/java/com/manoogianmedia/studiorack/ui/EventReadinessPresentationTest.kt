package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EventReadinessPresentationTest {
    @Test
    fun `summary uses decisions and blockers rather than a percentage`() {
        val rows = listOf(
            JSONObject().put("id", "a").put("importance", "critical").put("status", "ready").put("applicability", "applicable"),
            JSONObject().put("id", "b").put("importance", "required").put("status", "at_risk").put("applicability", "applicable"),
            JSONObject().put("id", "c").put("importance", "advisory").put("status", "not_started").put("applicability", "not_applicable"),
        )

        val summary = eventReadinessSummary(rows)

        assertEquals("at_risk", summary.status)
        assertEquals(1, summary.ready)
        assertEquals(1, summary.blockers)
        assertEquals(1, summary.notApplicable)
        assertFalse(summary.label.contains("%"))
    }

    @Test
    fun `dependency remains visible as a blocker even when its item says ready`() {
        val rows = listOf(
            JSONObject().put("id", "people").put("importance", "critical").put("status", "not_started").put("applicability", "applicable"),
            JSONObject().put("id", "music").put("importance", "critical").put("status", "ready").put("applicability", "applicable").put("depends_on_requirement_id", "people"),
        )

        val summary = eventReadinessSummary(rows)

        assertEquals("in_progress", summary.status)
        assertEquals(2, summary.blockers)
    }

    @Test
    fun `required evidence and approval keep a ready item blocked`() {
        val rows = listOf(
            JSONObject()
                .put("id", "advance")
                .put("importance", "critical")
                .put("status", "ready")
                .put("applicability", "applicable")
                .put("evidence_required", true)
                .put("evidence_text", "")
                .put("approval_status", "pending"),
        )

        val summary = eventReadinessSummary(rows)

        assertEquals("in_progress", summary.status)
        assertEquals(1, summary.blockers)
    }
}
