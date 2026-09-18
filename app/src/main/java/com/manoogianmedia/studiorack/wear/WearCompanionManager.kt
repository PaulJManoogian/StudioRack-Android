package com.manoogianmedia.studiorack.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val LEVIATHAN_WEAR_CAPABILITY = "leviathan_live_watch"

data class WearCompanionStatus(
    val loading: Boolean = true,
    val connectedDevices: List<WearDevice> = emptyList(),
    val installedNodeIds: Set<String> = emptySet(),
    val error: String = "",
) {
    val pairedCount: Int get() = connectedDevices.size
    val installedCount: Int get() = connectedDevices.count { it.id in installedNodeIds }
    val missingDevices: List<WearDevice> get() = connectedDevices.filterNot { it.id in installedNodeIds }
    val isReady: Boolean get() = installedCount > 0
}

data class WearDevice(val id: String, val name: String, val nearby: Boolean)

object WearCompanionManager {
    private val mutableStatus = MutableStateFlow(WearCompanionStatus())
    val status: StateFlow<WearCompanionStatus> = mutableStatus.asStateFlow()

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        mutableStatus.value = mutableStatus.value.copy(loading = true, error = "")
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes -> loadCapabilities(appContext, nodes) }
            .addOnFailureListener { failure ->
                mutableStatus.value = WearCompanionStatus(
                    loading = false,
                    error = failure.localizedMessage ?: "Wear OS connection could not be checked.",
                )
            }
    }

    private fun loadCapabilities(context: Context, nodes: List<Node>) {
        Wearable.getCapabilityClient(context)
            .getCapability(LEVIATHAN_WEAR_CAPABILITY, CapabilityClient.FILTER_ALL)
            .addOnSuccessListener { capability ->
                mutableStatus.value = WearCompanionStatus(
                    loading = false,
                    connectedDevices = nodes.map { WearDevice(it.id, it.displayName, it.isNearby) },
                    installedNodeIds = capability.nodes.mapTo(mutableSetOf()) { it.id },
                )
            }
            .addOnFailureListener { failure ->
                mutableStatus.value = WearCompanionStatus(
                    loading = false,
                    connectedDevices = nodes.map { WearDevice(it.id, it.displayName, it.isNearby) },
                    error = failure.localizedMessage ?: "The Wear companion installation could not be checked.",
                )
            }
    }

    fun openPlayStoreOnMissingWatches(
        context: Context,
        onResult: (String) -> Unit,
    ) {
        val targets = mutableStatus.value.missingDevices
        if (targets.isEmpty()) {
            onResult(if (mutableStatus.value.isReady) "The Wear companion is already installed." else "Connect a Wear OS watch first.")
            return
        }
        val helper = RemoteActivityHelper(context.applicationContext, ContextCompat.getMainExecutor(context))
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        var completed = 0
        var failures = 0
        targets.forEach { device ->
            val future = helper.startRemoteActivity(intent, device.id)
            future.addListener({
                completed += 1
                if (runCatching { future.get() }.isFailure) failures += 1
                if (completed == targets.size) {
                    onResult(
                        if (failures == 0) "Installation opened on ${targets.size} watch${if (targets.size == 1) "" else "es"}."
                        else "The Play Store could not be opened on $failures connected watch${if (failures == 1) "" else "es"}.",
                    )
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}
