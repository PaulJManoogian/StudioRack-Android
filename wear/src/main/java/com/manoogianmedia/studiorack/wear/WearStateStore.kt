package com.manoogianmedia.studiorack.wear

import android.content.Context
import android.util.Base64
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.Node
import com.manoogianmedia.studiorack.liveprotocol.LIVE_STATE_PATH
import com.manoogianmedia.studiorack.liveprotocol.LiveProtocol
import com.manoogianmedia.studiorack.liveprotocol.LiveSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearStateStore {
    private val mutableSnapshot = MutableStateFlow(LiveSnapshot())
    val snapshot: StateFlow<LiveSnapshot> = mutableSnapshot.asStateFlow()
    private val mutablePhoneConnected = MutableStateFlow(false)
    val phoneConnected: StateFlow<Boolean> = mutablePhoneConnected.asStateFlow()

    fun initialize(context: Context) {
        val saved = context.getSharedPreferences("leviathan_wear", Context.MODE_PRIVATE).getString("snapshot", null)
        saved?.let { encoded ->
            runCatching { LiveProtocol.decodeSnapshot(Base64.decode(encoded, Base64.NO_WRAP)) }
                .getOrNull()?.let { mutableSnapshot.value = it }
        }
        Wearable.getDataClient(context).dataItems.addOnSuccessListener { items ->
            items.use { buffer ->
                for (item in buffer) if (item.uri.path == LIVE_STATE_PATH) accept(context, DataMapItem.fromDataItem(item).dataMap.getByteArray("snapshot"))
            }
        }
        refreshConnection(context)
    }

    fun accept(context: Context, payload: ByteArray?) {
        val decoded = payload?.let { runCatching { LiveProtocol.decodeSnapshot(it) }.getOrNull() } ?: return
        if (decoded.revision < mutableSnapshot.value.revision && decoded.eventId == mutableSnapshot.value.eventId) return
        mutableSnapshot.value = decoded
        context.getSharedPreferences("leviathan_wear", Context.MODE_PRIVATE).edit()
            .putString("snapshot", Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    fun preview(snapshot: LiveSnapshot) {
        mutableSnapshot.value = snapshot
    }

    fun connected(connected: Boolean) {
        mutablePhoneConnected.value = connected
    }

    fun refreshConnection(context: Context) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { mutablePhoneConnected.value = it.isNotEmpty() }
            .addOnFailureListener { mutablePhoneConnected.value = false }
    }
}

class WearStateListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == LIVE_STATE_PATH) {
                WearStateStore.accept(this, DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("snapshot"))
            }
        }
    }

    override fun onPeerConnected(peer: Node) {
        WearStateStore.connected(true)
    }

    override fun onPeerDisconnected(peer: Node) {
        WearStateStore.refreshConnection(this)
    }
}
