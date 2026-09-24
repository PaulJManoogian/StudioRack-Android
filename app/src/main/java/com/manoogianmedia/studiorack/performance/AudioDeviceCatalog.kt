package com.manoogianmedia.studiorack.performance

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import org.json.JSONObject

data class LiveAudioDevice(
    val id: Int,
    val name: String,
    val kind: String,
    val outputChannelCounts: List<Int>,
    val sampleRates: List<Int>,
    val encodings: List<Int>,
) {
    val maximumOutputChannels: Int get() = outputChannelCounts.maxOrNull()?.coerceAtLeast(2) ?: 2
    val routingPreferenceKey: String get() = "$kind|$name|$maximumOutputChannels"
}

data class CompatibleRoutingProfile(
    val id: String,
    val name: String,
    val outputChannelCount: Int,
    val exactMatch: Boolean,
)

object AudioDeviceCatalog {
    fun outputInfo(context: Context, deviceId: Int): AudioDeviceInfo? {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == deviceId }
    }

    fun outputs(context: Context): List<LiveAudioDevice> {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .map { device ->
                LiveAudioDevice(
                    id = device.id,
                    name = device.productName?.toString()?.ifBlank { typeName(device.type) } ?: typeName(device.type),
                    kind = typeName(device.type),
                    outputChannelCounts = device.channelCounts.filter { it > 0 }.distinct().sorted(),
                    sampleRates = device.sampleRates.filter { it > 0 }.distinct().sorted(),
                    encodings = device.encodings.distinct().sorted(),
                )
            }
            .sortedWith(compareByDescending<LiveAudioDevice> { it.kind == "USB audio" }.thenBy { it.name.lowercase() })
    }

    fun compatibleProfile(device: LiveAudioDevice, profiles: List<JSONObject>): CompatibleRoutingProfile? {
        return compatibleProfiles(device, profiles).firstOrNull()
    }

    fun compatibleProfiles(device: LiveAudioDevice, profiles: List<JSONObject>): List<CompatibleRoutingProfile> {
        return compatibleProfilesForChannels(device.maximumOutputChannels, profiles)
    }

    fun compatibleProfileForChannels(availableChannels: Int, profiles: List<JSONObject>): CompatibleRoutingProfile? {
        return compatibleProfilesForChannels(availableChannels, profiles).firstOrNull()
    }

    fun compatibleProfilesForChannels(availableChannels: Int, profiles: List<JSONObject>): List<CompatibleRoutingProfile> {
        val available = availableChannels.coerceAtLeast(2)
        val candidates = profiles.mapNotNull { profile ->
            val count = profile.optInt("output_channel_count", 2).coerceAtLeast(2)
            if (count > available) null else CompatibleRoutingProfile(
                id = profile.optString("id"),
                name = profile.optString("name", "$count-output profile"),
                outputChannelCount = count,
                exactMatch = count == available,
            ) to profile.optBoolean("is_default")
        }
        return candidates.sortedWith(
            compareByDescending<Pair<CompatibleRoutingProfile, Boolean>> { it.first.exactMatch }
                .thenByDescending { it.second }
                .thenByDescending { it.first.outputChannelCount }
        ).map { it.first }
    }

    private fun typeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB audio"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth audio"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_LINE_ANALOG -> "Wired audio"
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI audio"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in audio"
        else -> "Audio output"
    }
}
