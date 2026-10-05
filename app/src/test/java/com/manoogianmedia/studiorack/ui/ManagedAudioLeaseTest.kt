package com.manoogianmedia.studiorack.ui

import com.manoogianmedia.studiorack.data.SupportingRecord
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedAudioLeaseTest {
    @Test
    fun missingPolicyPreservesCompatibility() {
        assertTrue(managedAudioLeaseIsActive(emptyList(), Instant.parse("2026-10-05T12:00:00Z")))
    }

    @Test
    fun expiredManagedLeaseIsRejected() {
        val policy = listOf(
            SupportingRecord(
                "storage_policy",
                "managed_storage",
                """{"offline_lease_expires_utc":"2026-10-04T12:00:00Z"}""",
            ),
        )
        assertFalse(managedAudioLeaseIsActive(policy, Instant.parse("2026-10-05T12:00:00Z")))
    }

    @Test
    fun currentManagedLeaseIsAccepted() {
        val policy = listOf(
            SupportingRecord(
                "storage_policy",
                "managed_storage",
                """{"offline_lease_expires_utc":"2026-11-04T12:00:00Z"}""",
            ),
        )
        assertTrue(managedAudioLeaseIsActive(policy, Instant.parse("2026-10-05T12:00:00Z")))
    }
}
