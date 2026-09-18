package com.manoogianmedia.studiorack.liveprotocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

const val LIVE_STATE_PATH = "/leviathan/live/state"
const val LIVE_COMMAND_PATH = "/leviathan/live/command"

data class LiveSong(
    val title: String = "",
    val artist: String = "",
    val key: String = "",
    val tempo: String = "",
    val timeSignature: String = "",
    val startsBy: String = "",
    val groupType: String = "",
    val groupName: String = "",
    val groupPosition: Int = 0,
    val groupCount: Int = 0,
)

data class LiveSnapshot(
    val active: Boolean = false,
    val eventId: String = "",
    val eventTitle: String = "",
    val setListName: String = "",
    val sectionName: String = "",
    val currentIndex: Int = 0,
    val totalSongs: Int = 0,
    val current: LiveSong = LiveSong(),
    val previous: LiveSong = LiveSong(),
    val next: LiveSong = LiveSong(),
    val metronomeRunning: Boolean = false,
    val metronomeMuted: Boolean = false,
    val metronomeTempo: Int = 120,
    val beatsPerMeasure: Int = 4,
    val metronomeStartedAtEpochMs: Long = 0,
    val lastCommandId: String = "",
    val revision: Long = 0,
)

enum class LiveCommandType { PREVIOUS, NEXT, TOGGLE_METRONOME, TOGGLE_MUTE }

data class LiveCommand(val id: String, val type: LiveCommandType)

object LiveProtocol {
    private const val VERSION = 1

    fun encode(snapshot: LiveSnapshot): ByteArray = bytes { output ->
        output.writeInt(VERSION)
        output.writeBoolean(snapshot.active)
        output.writeUtf8(snapshot.eventId)
        output.writeUtf8(snapshot.eventTitle)
        output.writeUtf8(snapshot.setListName)
        output.writeUtf8(snapshot.sectionName)
        output.writeInt(snapshot.currentIndex)
        output.writeInt(snapshot.totalSongs)
        output.writeSong(snapshot.current)
        output.writeSong(snapshot.previous)
        output.writeSong(snapshot.next)
        output.writeBoolean(snapshot.metronomeRunning)
        output.writeBoolean(snapshot.metronomeMuted)
        output.writeInt(snapshot.metronomeTempo)
        output.writeInt(snapshot.beatsPerMeasure)
        output.writeLong(snapshot.metronomeStartedAtEpochMs)
        output.writeUtf8(snapshot.lastCommandId)
        output.writeLong(snapshot.revision)
    }

    fun decodeSnapshot(bytes: ByteArray): LiveSnapshot = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        check(input.readInt() == VERSION) { "Unsupported Leviathan Live protocol version." }
        LiveSnapshot(
            active = input.readBoolean(),
            eventId = input.readUtf8(),
            eventTitle = input.readUtf8(),
            setListName = input.readUtf8(),
            sectionName = input.readUtf8(),
            currentIndex = input.readInt(),
            totalSongs = input.readInt(),
            current = input.readSong(),
            previous = input.readSong(),
            next = input.readSong(),
            metronomeRunning = input.readBoolean(),
            metronomeMuted = input.readBoolean(),
            metronomeTempo = input.readInt(),
            beatsPerMeasure = input.readInt(),
            metronomeStartedAtEpochMs = input.readLong(),
            lastCommandId = input.readUtf8(),
            revision = input.readLong(),
        )
    }

    fun encode(command: LiveCommand): ByteArray = bytes { output ->
        output.writeInt(VERSION)
        output.writeUtf8(command.id)
        output.writeUtf8(command.type.name)
    }

    fun decodeCommand(bytes: ByteArray): LiveCommand = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        check(input.readInt() == VERSION) { "Unsupported Leviathan Live protocol version." }
        LiveCommand(input.readUtf8(), LiveCommandType.valueOf(input.readUtf8()))
    }

    private fun bytes(write: (DataOutputStream) -> Unit): ByteArray = ByteArrayOutputStream().use { buffer ->
        DataOutputStream(buffer).use(write)
        buffer.toByteArray()
    }

    private fun DataOutputStream.writeSong(song: LiveSong) {
        writeUtf8(song.title)
        writeUtf8(song.artist)
        writeUtf8(song.key)
        writeUtf8(song.tempo)
        writeUtf8(song.timeSignature)
        writeUtf8(song.startsBy)
        writeUtf8(song.groupType)
        writeUtf8(song.groupName)
        writeInt(song.groupPosition)
        writeInt(song.groupCount)
    }

    private fun DataInputStream.readSong() = LiveSong(
        title = readUtf8(), artist = readUtf8(), key = readUtf8(), tempo = readUtf8(),
        timeSignature = readUtf8(), startsBy = readUtf8(), groupType = readUtf8(),
        groupName = readUtf8(), groupPosition = readInt(), groupCount = readInt(),
    )

    private fun DataOutputStream.writeUtf8(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataInputStream.readUtf8(): String {
        val size = readInt()
        require(size in 0..1_000_000) { "Invalid string size." }
        return ByteArray(size).also(::readFully).toString(Charsets.UTF_8)
    }
}
