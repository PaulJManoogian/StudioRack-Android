package com.manoogianmedia.studiorack.ui

import com.manoogianmedia.studiorack.data.NotificationReceipt
import com.manoogianmedia.studiorack.data.SyncConflict
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertsPresentationTest {
    @Test
    fun conflictBadgeNavigatesToSyncResolution() {
        val badge = conflictBadgePresentation(2)

        assertEquals("2 CONFLICTS", badge.label)
        assertEquals("Sync", badge.destinationTab)
    }

    @Test
    fun conflictPresentationNamesLinkedSongAndChangedFields() {
        val conflict = SyncConflict(
            mutationId = "mutation_1",
            entityType = "set_list_entry",
            entityId = "sle_1",
            localJson = JSONObject().put("song_id", "song_1").put("position", 3).toString(),
            serverJson = JSONObject().put("song_id", "song_1").put("position", 4).toString(),
            serverRevision = 2,
        )

        val presentation = conflictPresentation(conflict, mapOf("song_1" to "Midnight Signal"))

        assertEquals("Midnight Signal", presentation.recordName)
        assertEquals("Set List Entry", presentation.recordType)
        assertEquals("Different fields: Position", presentation.changedFields)
    }

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
