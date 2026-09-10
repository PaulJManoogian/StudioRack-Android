package com.manoogianmedia.studiorack.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

enum class LocalLiveRole { NONE, HOST, GUEST }

data class LocalLivePeer(val name: String, val address: String, val port: Int) {
    val key: String get() = "$address:$port"
}

data class LocalLiveState(
    val role: LocalLiveRole = LocalLiveRole.NONE,
    val eventId: String = "",
    val setListId: String = "",
    val sessionName: String = "",
    val address: String = "",
    val code: String = "",
    val connected: Boolean = false,
    val peerCount: Int = 0,
    val revision: Long = 0,
    val lastUpdateAt: Long? = null,
    val error: String? = null,
)

class LocalLiveCoordinator(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _state = MutableStateFlow(LocalLiveState())
    val state: StateFlow<LocalLiveState> = _state.asStateFlow()
    private val _peers = MutableStateFlow<List<LocalLivePeer>>(emptyList())
    val peers: StateFlow<List<LocalLivePeer>> = _peers.asStateFlow()
    private val found = linkedMapOf<String, LocalLivePeer>()
    private val revision = AtomicLong(1)
    private var server: LocalLiveServer? = null
    private var registration: NsdManager.RegistrationListener? = null
    private var pollJob: Job? = null
    private var packetProvider: (suspend () -> JSONObject)? = null
    private var incomingSetList: (suspend (JSONObject) -> Unit)? = null
    private var packetConsumer: (suspend (JSONObject) -> Unit)? = null

    init { startDiscovery() }

    suspend fun host(
        eventId: String,
        sessionName: String,
        providePacket: suspend () -> JSONObject,
        acceptSetList: suspend (JSONObject) -> Unit,
    ) {
        stop()
        packetProvider = providePacket
        incomingSetList = acceptSetList
        val initial = providePacket()
        val code = (100000..999999).random().toString()
        val localServer = LocalLiveServer(0, code)
        server = localServer
        localServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        val address = localAddress()
        _state.value = LocalLiveState(
            role = LocalLiveRole.HOST,
            eventId = eventId,
            setListId = initial.optString("set_list_id"),
            sessionName = sessionName,
            address = "$address:${localServer.listeningPort}",
            code = code,
            connected = true,
            revision = revision.get(),
            lastUpdateAt = System.currentTimeMillis(),
        )
        register(sessionName, localServer.listeningPort)
    }

    fun hostChanged() {
        if (_state.value.role != LocalLiveRole.HOST) return
        val next = revision.incrementAndGet()
        _state.value = _state.value.copy(revision = next, lastUpdateAt = System.currentTimeMillis(), error = null)
    }

    fun join(peer: LocalLivePeer, code: String, consumePacket: suspend (JSONObject) -> Unit) {
        stop()
        packetConsumer = consumePacket
        _state.value = LocalLiveState(
            role = LocalLiveRole.GUEST,
            sessionName = peer.name.removePrefix("Leviathan - "),
            address = peer.key,
            code = code.trim(),
        )
        pollJob = scope.launch {
            while (isActive) {
                runCatching { requestPacket(peer, code.trim()) }
                    .onSuccess { packet ->
                        val incomingRevision = packet.optLong("local_live_revision")
                        if (!_state.value.connected || incomingRevision != _state.value.revision) consumePacket(packet)
                        _state.value = _state.value.copy(
                            eventId = packet.optString("event_id"),
                            setListId = packet.optString("set_list_id"),
                            connected = true,
                            revision = incomingRevision,
                            lastUpdateAt = System.currentTimeMillis(),
                            error = null,
                        )
                    }
                    .onFailure { error -> _state.value = _state.value.copy(connected = false, error = error.message ?: "Local host unavailable") }
                delay(900)
            }
        }
    }

    suspend fun submitSetList(payload: JSONObject): JSONObject {
        val current = _state.value
        check(current.role == LocalLiveRole.GUEST) { "This device is not joined to a local host." }
        val connection = open(current.address, "/set-list?code=${encode(current.code)}", "POST")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        return readResponse(connection).also { packet ->
            _state.value = _state.value.copy(
                connected = true,
                revision = packet.optLong("local_live_revision"),
                lastUpdateAt = System.currentTimeMillis(),
                error = null,
            )
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        server?.stop()
        server = null
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
        packetProvider = null
        incomingSetList = null
        packetConsumer = null
        _state.value = LocalLiveState()
    }

    private inner class LocalLiveServer(port: Int, private val accessCode: String) : NanoHTTPD(port) {
        private val visitors = mutableMapOf<String, Long>()
        private val failedCodes = mutableMapOf<String, Pair<Int, Long>>()

        override fun serve(session: IHTTPSession): Response {
            val code = session.parameters["code"]?.firstOrNull().orEmpty()
            val remote = session.remoteIpAddress.orEmpty()
            synchronized(failedCodes) {
                val failure = failedCodes[remote]
                if (failure != null && failure.second > System.currentTimeMillis()) {
                    return json(Response.Status.FORBIDDEN, JSONObject().put("error", "Too many incorrect codes. Try again in one minute."))
                }
                if (code != accessCode) {
                    val attempts = (failure?.first ?: 0) + 1
                    failedCodes[remote] = attempts to if (attempts >= 5) System.currentTimeMillis() + 60_000 else 0L
                    return json(Response.Status.UNAUTHORIZED, JSONObject().put("error", "Invalid session code"))
                }
                failedCodes.remove(remote)
            }
            val active = synchronized(visitors) {
                visitors[remote] = System.currentTimeMillis()
                visitors.entries.removeAll { System.currentTimeMillis() - it.value >= 10_000 }
                visitors.size
            }
            _state.value = _state.value.copy(peerCount = active, lastUpdateAt = System.currentTimeMillis())
            return try {
                when {
                    session.method == Method.GET && session.uri == "/packet" -> json(Response.Status.OK, hostPacket())
                    session.method == Method.POST && session.uri == "/set-list" -> {
                        val files = HashMap<String, String>()
                        session.parseBody(files)
                        val payload = JSONObject(files["postData"].orEmpty())
                        runBlocking { incomingSetList?.invoke(payload) ?: error("The host is no longer accepting edits.") }
                        hostChanged()
                        json(Response.Status.OK, hostPacket())
                    }
                    else -> json(Response.Status.NOT_FOUND, JSONObject().put("error", "Not found"))
                }
            } catch (error: Exception) {
                json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", error.message ?: "Local live request failed"))
            }
        }

        private fun hostPacket(): JSONObject = runBlocking {
            (packetProvider?.invoke() ?: error("The host packet is unavailable."))
                .put("local_live_revision", revision.get())
        }

        private fun json(status: Response.Status, body: JSONObject): Response =
            newFixedLengthResponse(status, "application/json", body.toString()).apply { addHeader("Cache-Control", "no-store") }
    }

    private fun requestPacket(peer: LocalLivePeer, code: String): JSONObject =
        readResponse(open(peer.key, "/packet?code=${encode(code)}", "GET"))

    private fun open(authority: String, path: String, method: String): HttpURLConnection =
        (URL("http://$authority$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 2_000
            readTimeout = 3_000
            setRequestProperty("Accept", "application/json")
        }

    private fun readResponse(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error(runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty().ifBlank { "Local host returned $code" })
        return JSONObject(body)
    }

    private fun register(name: String, port: Int) {
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) { _state.value = _state.value.copy(error = "Local discovery could not be advertised ($code). The address still works.") }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) = Unit
        }
        registration = listener
        val info = NsdServiceInfo().apply {
            serviceName = "Leviathan - ${name.take(28)}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    @Suppress("DEPRECATION")
    private fun startDiscovery() {
        runCatching {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(type: String) = Unit
                override fun onDiscoveryStopped(type: String) = Unit
                override fun onStartDiscoveryFailed(type: String, code: Int) = Unit
                override fun onStopDiscoveryFailed(type: String, code: Int) = Unit
                override fun onServiceLost(info: NsdServiceInfo) {
                    synchronized(found) {
                        found.entries.removeAll { it.value.name == info.serviceName }
                        _peers.value = found.values.toList()
                    }
                }
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceType != SERVICE_TYPE) return
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val address = serviceInfo.host?.hostAddress ?: return
                            val peer = LocalLivePeer(serviceInfo.serviceName, address, serviceInfo.port)
                            synchronized(found) {
                                found[peer.key] = peer
                                _peers.value = found.values.sortedBy { it.name }
                            }
                        }
                    })
                }
            })
        }
    }

    private fun localAddress(): String {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivity.allNetworks.forEach { network ->
            val capabilities = connectivity.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                connectivity.getLinkProperties(network)?.linkAddresses
                    ?.map { it.address }
                    ?.filterIsInstance<Inet4Address>()
                    ?.firstOrNull { !it.isLoopbackAddress }
                    ?.hostAddress
                    ?.let { return it }
            }
        }
        return Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("ap") || it.name.startsWith("swlan")) 0 else 1 }
            .flatMap { Collections.list(it.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress ?: "Local network"
    }

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object { private const val SERVICE_TYPE = "_leviathan._tcp." }
}
