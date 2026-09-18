package com.manoogianmedia.studiorack.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.MessageEvent
import com.manoogianmedia.studiorack.liveprotocol.LIVE_COMMAND_PATH
import com.manoogianmedia.studiorack.liveprotocol.LIVE_STATE_PATH
import com.manoogianmedia.studiorack.liveprotocol.LiveCommand
import com.manoogianmedia.studiorack.liveprotocol.LiveProtocol
import com.manoogianmedia.studiorack.liveprotocol.LiveSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LiveWearBridge {
    private val mutableCommands = MutableSharedFlow<LiveCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<LiveCommand> = mutableCommands.asSharedFlow()

    fun receive(payload: ByteArray) {
        runCatching { LiveProtocol.decodeCommand(payload) }.getOrNull()?.let(mutableCommands::tryEmit)
    }

    fun publish(context: Context, snapshot: LiveSnapshot) {
        val request = PutDataMapRequest.create(LIVE_STATE_PATH).apply {
            dataMap.putByteArray("snapshot", LiveProtocol.encode(snapshot))
            dataMap.putLong("revision", snapshot.revision)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context.applicationContext).putDataItem(request)
    }
}

class LiveWearListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == LIVE_COMMAND_PATH) LiveWearBridge.receive(event.data)
    }
}
