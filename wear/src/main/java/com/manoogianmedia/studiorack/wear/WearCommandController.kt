package com.manoogianmedia.studiorack.wear

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.Wearable
import com.manoogianmedia.studiorack.liveprotocol.LIVE_COMMAND_PATH
import com.manoogianmedia.studiorack.liveprotocol.LiveCommand
import com.manoogianmedia.studiorack.liveprotocol.LiveCommandType
import com.manoogianmedia.studiorack.liveprotocol.LiveProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class CommandStatus(val pendingId: String = "", val message: String = "")

class WearCommandController(private val context: Context) {
    private val mutableStatus = MutableStateFlow(CommandStatus())
    private val handler = Handler(Looper.getMainLooper())
    val status: StateFlow<CommandStatus> = mutableStatus.asStateFlow()

    fun send(type: LiveCommandType) {
        val command = LiveCommand(UUID.randomUUID().toString(), type)
        mutableStatus.value = CommandStatus(command.id, "Sending")
        handler.postDelayed({
            if (mutableStatus.value.pendingId == command.id) {
                mutableStatus.value = CommandStatus(message = "No response from Leviathan Live")
            }
        }, 3_500)
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    mutableStatus.value = CommandStatus(message = "Phone disconnected")
                    return@addOnSuccessListener
                }
                val payload = LiveProtocol.encode(command)
                nodes.forEach { node ->
                    Wearable.getMessageClient(context).sendMessage(node.id, LIVE_COMMAND_PATH, payload)
                        .addOnFailureListener { mutableStatus.value = CommandStatus(message = "Command not delivered") }
                }
            }
            .addOnFailureListener { mutableStatus.value = CommandStatus(message = "Phone unavailable") }
    }

    fun acknowledge(commandId: String) {
        if (commandId.isNotBlank() && commandId == mutableStatus.value.pendingId) {
            mutableStatus.value = CommandStatus(message = "Applied")
        }
    }
}
