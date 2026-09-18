package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMapLinkTest {
    @Test
    fun `saved venue map link takes precedence`() {
        val event = JSONObject().put("location", "Back room")
        val venue = JSONObject()
            .put("name", "The Venue")
            .put("maps_url", "https://maps.example.test/place")
            .put("address_line1", "100 Main Street")

        assertEquals("https://maps.example.test/place", sessionMapLink(event, venue))
    }

    @Test
    fun `venue identity and address drive fallback map search`() {
        val event = JSONObject().put("location", "Back room")
        val venue = JSONObject()
            .put("name", "The Venue")
            .put("address_line1", "100 Main Street")
            .put("city", "Newtown")
            .put("region", "PA")
            .put("postal_code", "18940")

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=The%20Venue%2C%20100%20Main%20Street%2C%20Newtown%2C%20PA%2C%2018940",
            sessionMapLink(event, venue),
        )
    }

    @Test
    fun `unlisted session location is used without a venue`() {
        val event = JSONObject().put("location", "25 Stage Door Road, Philadelphia, PA")
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=25%20Stage%20Door%20Road%2C%20Philadelphia%2C%20PA",
            sessionMapLink(event, null),
        )
        assertNull(sessionMapLink(JSONObject(), null))
    }
}
