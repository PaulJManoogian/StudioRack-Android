package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformanceGroupTest {
    @Test
    fun jsonNullsDoNotCreateAVisibleGroup() {
        val entry = JSONObject()
            .put("performance_group_id", JSONObject.NULL)
            .put("performance_group_type", JSONObject.NULL)
            .put("performance_group_name", JSONObject.NULL)

        assertNull(entry.performanceGroupOrNull())
    }

    @Test
    fun onlyNamedMedleyOrTributeGroupsAreVisible() {
        val medley = JSONObject()
            .put("performance_group_id", "grp_1")
            .put("performance_group_type", "medley")
            .put("performance_group_name", "Opener")
        val invalid = JSONObject()
            .put("performance_group_id", "grp_2")
            .put("performance_group_type", "collection")
            .put("performance_group_name", "Everything")

        assertEquals(PerformanceGroup("grp_1", "medley", "Opener"), medley.performanceGroupOrNull())
        assertNull(invalid.performanceGroupOrNull())
    }
}
