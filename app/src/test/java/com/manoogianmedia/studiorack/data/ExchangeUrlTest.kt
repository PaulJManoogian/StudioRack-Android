package com.manoogianmedia.studiorack.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeUrlTest {
    @Test
    fun selectedIdsAreSentInVisibleOrder() {
        assertEquals(
            "https://example.test/api/exchange/setlists.csv?ids=set_2%2Cset_1",
            exchangeUrl("https://example.test/api", "setlists", "csv", listOf("set_2", "set_1")),
        )
    }

    @Test
    fun emptySelectionKeepsTheAllRecordsEndpoint() {
        assertEquals(
            "https://example.test/api/exchange/songs.json",
            exchangeUrl("https://example.test/api", "songs", "json", emptyList()),
        )
    }
}
