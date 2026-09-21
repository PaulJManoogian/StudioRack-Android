package com.manoogianmedia.studiorack.performance

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import org.json.JSONObject

data class MidiDestination(
    val deviceId: Int,
    val portNumber: Int,
    val label: String,
) {
    val key: String = "$deviceId:$portNumber"
}

class MidiOutputRouter(private val context: Context) : AutoCloseable {
    private val manager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private var device: MidiDevice? = null
    private var port: MidiInputPort? = null
    private var openKey: String = ""

    val supported: Boolean
        get() = manager != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)

    fun destinations(): List<MidiDestination> = manager?.devices.orEmpty().flatMap { info ->
        val properties = info.properties
        val deviceName = properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
            ?: "MIDI device"
        info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }.map { port ->
            MidiDestination(info.id, port.portNumber, listOf(deviceName, port.name).filterNotNull().filter(String::isNotBlank).joinToString(" - "))
        }
    }.sortedBy { it.label.lowercase() }

    fun selectedKey(): String = preferences.getString(SELECTED_DESTINATION, "").orEmpty()

    fun select(destination: MidiDestination?) {
        val key = destination?.key.orEmpty()
        preferences.edit().putString(SELECTED_DESTINATION, key).apply()
        if (key != openKey) closeConnection()
    }

    fun send(payload: JSONObject, onResult: (Boolean) -> Unit = {}) {
        val destination = destinations().firstOrNull { it.key == selectedKey() }
        if (destination == null || manager == null) {
            onResult(false)
            return
        }
        val bytes = midiBytes(payload) ?: run {
            onResult(false)
            return
        }
        if (openKey == destination.key && port != null) {
            onResult(sendBytes(bytes))
            return
        }
        closeConnection()
        val info = manager.devices.firstOrNull { it.id == destination.deviceId }
        if (info == null) {
            onResult(false)
            return
        }
        manager.openDevice(info, { opened ->
            if (opened == null) {
                onResult(false)
                return@openDevice
            }
            device = opened
            port = opened.openInputPort(destination.portNumber)
            openKey = if (port != null) destination.key else ""
            onResult(sendBytes(bytes))
        }, null)
    }

    private fun sendBytes(bytes: ByteArray): Boolean = runCatching {
        val receiver = port ?: return false
        receiver.send(bytes, 0, bytes.size, System.nanoTime())
        true
    }.getOrDefault(false)

    private fun midiBytes(payload: JSONObject): ByteArray? {
        val channel = (payload.optInt("channel", 1).coerceIn(1, 16) - 1)
        val number = payload.optInt("number", 0).coerceIn(0, 127)
        val value = payload.optInt("value", 0).coerceIn(0, 127)
        return when (payload.optString("command")) {
            "program_change" -> byteArrayOf((0xC0 or channel).toByte(), number.toByte())
            "control_change" -> byteArrayOf((0xB0 or channel).toByte(), number.toByte(), value.toByte())
            "note_on" -> byteArrayOf((0x90 or channel).toByte(), number.toByte(), value.toByte())
            "note_off" -> byteArrayOf((0x80 or channel).toByte(), number.toByte(), value.toByte())
            else -> null
        }
    }

    private fun closeConnection() {
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        openKey = ""
    }

    override fun close() = closeConnection()

    companion object {
        private const val PREFERENCES = "leviathan_live_midi"
        private const val SELECTED_DESTINATION = "selected_destination"
    }
}
