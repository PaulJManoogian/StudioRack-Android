package com.manoogianmedia.studiorack.ui

import com.manoogianmedia.studiorack.data.NotificationReceipt
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertsPresentationTest {
    @Test
    fun crewReceiptUsesTenantFacingContentAndDestinationArea() {
        val receipt = NotificationReceipt(
            sourceId = "sba_deadbeef",
            fingerprint = "fingerprint",
            notificationId = 42,
            destination = "equipment",
            recordId = "item_tom",
        )
        val action = JSONObject()
            .put("display_subject", "12-inch tom needs attention")
            .put("body", "Please review this maintenance item.\nReference: SL-sba_deadbeef")

        val result = alertPresentation(
            receipt = receipt,
            actions = mapOf(receipt.sourceId to action),
            notes = emptyMap(),
            events = emptyMap(),
            itemNames = emptyMap(),
            shares = emptyMap(),
        )

        assertEquals("12-inch tom needs attention", result.title)
        assertEquals("Please review this maintenance item.", result.body)
        assertEquals("Gear", result.area)
    }
}
