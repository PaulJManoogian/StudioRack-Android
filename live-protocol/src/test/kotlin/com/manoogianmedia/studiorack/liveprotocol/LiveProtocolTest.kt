package com.manoogianmedia.studiorack.liveprotocol

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveProtocolTest {
    @Test
    fun snapshotRoundTripPreservesPerformanceState() {
        val snapshot = LiveSnapshot(
            active = true,
            eventId = "event-1",
            setListName = "Main Set",
            sectionName = "Set One",
            currentIndex = 2,
            totalSongs = 12,
            current = LiveSong("China Grove", "The Doobie Brothers", "A", "146", "4/4", "Guitar"),
            next = LiveSong("Rain", "The Beatles", "G", "120", "4/4", "Drums", "medley", "Closer", 1, 2),
            metronomeRunning = true,
            metronomeTempo = 146,
            metronomeStartedAtEpochMs = 123456L,
            lastCommandId = "command-1",
            revision = 9,
        )

        assertEquals(snapshot, LiveProtocol.decodeSnapshot(LiveProtocol.encode(snapshot)))
    }

    @Test
    fun commandRoundTripPreservesIdentity() {
        val command = LiveCommand("command-2", LiveCommandType.NEXT)
        assertEquals(command, LiveProtocol.decodeCommand(LiveProtocol.encode(command)))
    }
}
