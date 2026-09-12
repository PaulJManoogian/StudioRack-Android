package com.manoogianmedia.studiorack.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StudioValueTest {
    @Test
    fun depreciatesPurchasePriceAcrossFiveYears() {
        val twoYearsAgo = LocalDate.now().minusDays(730).toString()

        val value = estimatedItemValue(
            mapOf("purchase_price" to "1000.00", "purchase_date" to twoYearsAgo),
        )

        assertEquals(600.27, value, 0.75)
    }

    @Test
    fun neverReturnsNegativeValueAfterFiveYears() {
        val value = estimatedItemValue(
            mapOf("purchase_price" to "1000.00", "purchase_date" to LocalDate.now().minusYears(8).toString()),
        )

        assertEquals(0.0, value, 0.0)
    }

    @Test
    fun excludesCuratedArtifactsFromStraightLineTotal() {
        val value = estimatedItemValue(
            mapOf("purchase_price" to "1000.00", "curated_artifact" to "yes"),
        )

        assertEquals(0.0, value, 0.0)
    }
}
