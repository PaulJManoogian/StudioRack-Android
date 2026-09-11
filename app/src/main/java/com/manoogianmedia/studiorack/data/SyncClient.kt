package com.manoogianmedia.studiorack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

class SyncClient(
    private val tokenStore: TokenStore,
    private val baseUrl: String,
    private val exportFilePrefix: String,
) {
    suspend fun signIn(email: String, accessCode: String, mfaCode: String, deviceName: String, deviceId: String): JSONObject =
        request(
            "/auth/token",
            "POST",
            JSONObject()
                .put("email", email)
                .put("access_code", accessCode)
                .put("mfa_code", mfaCode)
                .put("device_name", deviceName)
                .put("device_id", deviceId)
                .put("platform", "android"),
            authenticated = false,
        )

    suspend fun pull(cursor: Long): JSONObject = request("/sync/pull?cursor=$cursor&limit=500")

    suspend fun push(mutations: JSONArray): JSONObject =
        request("/sync/push", "POST", JSONObject().put("mutations", mutations))

    suspend fun updatePerformanceSettings(settings: JSONObject): JSONObject =
        request("/settings/performance", "PUT", settings)

    suspend fun liveEventStatus(eventId: String): JSONObject =
        request("/live/events/${URLEncoder.encode(eventId, Charsets.UTF_8.name())}/status")

    suspend fun liveShareStatus(grantId: String): JSONObject =
        request("/live/shares/${URLEncoder.encode(grantId, Charsets.UTF_8.name())}/status")

    suspend fun revoke() = request("/auth/revoke", "POST", JSONObject())

    suspend fun shareLink(grantId: String): JSONObject = request("/sharing/$grantId/link", "POST", JSONObject())

    suspend fun emailShare(grantId: String): JSONObject = request("/sharing/$grantId/email", "POST", JSONObject())

    suspend fun revokeShare(grantId: String): JSONObject = request("/sharing/$grantId/revoke", "POST", JSONObject())

    suspend fun updateShare(grantId: String, data: JSONObject): JSONObject = request("/sharing/$grantId/update", "POST", data)

    suspend fun reportOverview(): JSONObject = request("/reports/overview")

    suspend fun runAiReport(question: String): JSONObject =
        request("/reports/ai", "POST", JSONObject().put("question", question))

    suspend fun searchSongMetadata(title: String, artist: String): JSONObject =
        request(
            "/song-metadata/search?title=${URLEncoder.encode(title, Charsets.UTF_8.name())}&artist=${URLEncoder.encode(artist, Charsets.UTF_8.name())}"
        )

    suspend fun searchSongLyrics(title: String, artist: String, album: String): JSONObject =
        request(
            "/song-lyrics/search?title=${URLEncoder.encode(title, Charsets.UTF_8.name())}" +
                "&artist=${URLEncoder.encode(artist, Charsets.UTF_8.name())}" +
                "&album=${URLEncoder.encode(album, Charsets.UTF_8.name())}"
        )

    suspend fun structureSongLyrics(title: String, artist: String, album: String, lyrics: String): JSONObject =
        request(
            "/song-lyrics/structure",
            "POST",
            JSONObject().put("title", title).put("artist", artist).put("album", album).put("lyrics", lyrics),
        )

    suspend fun fillMissingSongLengths(): JSONObject =
        request("/song-durations/fill-missing", "POST", JSONObject())

    suspend fun applySongDuration(songId: String, candidate: JSONObject): JSONObject =
        request("/songs/${URLEncoder.encode(songId, Charsets.UTF_8.name())}/duration", "POST", candidate)

    suspend fun exportData(kind: String, format: String, ids: List<String> = emptyList()): DataExport = withContext(Dispatchers.IO) {
        val connection = URL(exchangeUrl(baseUrl, kind, format, ids)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            val token = tokenStore.token() ?: throw SyncException(401, "This device is signed out.")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            if (status !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw SyncException(status, runCatching { JSONObject(detail).optString("detail") }.getOrNull().orEmpty().ifBlank { "Data export failed." })
            }
            val disposition = connection.getHeaderField("Content-Disposition").orEmpty()
            val filename = Regex("filename=\"?([^\";]+)").find(disposition)?.groupValues?.get(1) ?: "$exportFilePrefix-$kind.$format"
            DataExport(filename, connection.contentType ?: "application/octet-stream", connection.inputStream.use { it.readBytes() })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun importData(kind: String, file: File, displayName: String, mimeType: String): JSONObject = withContext(Dispatchers.IO) {
        val boundary = "ApplicationExchange-${System.currentTimeMillis()}"
        val encodedKind = URLEncoder.encode(kind, Charsets.UTF_8.name())
        val connection = URL("$baseUrl/exchange/import?kind=$encodedKind").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            val token = tokenStore.token() ?: throw SyncException(401, "This device is signed out.")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "-").ifBlank { file.name }
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"upload\"; filename=\"$safeName\"\r\n")
                output.writeBytes("Content-Type: ${mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n")
                file.inputStream().use { it.copyTo(output) }
                output.writeBytes("\r\n--$boundary--\r\n")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (payload.isBlank()) JSONObject() else JSONObject(payload)
            if (status !in 200..299) throw SyncException(status, json.optString("detail", "Data import failed."))
            json
        } finally {
            connection.disconnect()
        }
    }

    suspend fun uploadAttachment(file: File, displayName: String, mimeType: String): JSONObject = withContext(Dispatchers.IO) {
        val boundary = "ApplicationAttachment-${System.currentTimeMillis()}"
        val connection = URL("$baseUrl/attachments/upload").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            val token = tokenStore.token() ?: throw SyncException(401, "This device is signed out.")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "-").ifBlank { file.name }
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"upload\"; filename=\"$safeName\"\r\n")
                output.writeBytes("Content-Type: ${mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n")
                file.inputStream().use { it.copyTo(output) }
                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (payload.isBlank()) JSONObject() else JSONObject(payload)
            if (status !in 200..299) throw SyncException(status, json.optString("detail", "Attachment upload failed."))
            json
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAttachment(path: String, destination: File): AttachmentDownload = withContext(Dispatchers.IO) {
        val url = URL(resolveDownloadUrl(baseUrl, path))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Accept", "application/pdf,image/*,application/json")
            val token = tokenStore.token() ?: throw SyncException(401, "This device is signed out.")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            if (status !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw SyncException(status, runCatching { JSONObject(detail).optString("detail") }.getOrNull().orEmpty().ifBlank { "Attachment download failed." })
            }
            val contentType = connection.contentType?.substringBefore(';')?.lowercase()
            if (contentType == "application/json") {
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                return@withContext AttachmentDownload(remoteUrl = json.optString("remote_url").takeIf(String::isNotBlank))
            }
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, destination.name + ".part")
            val digest = MessageDigest.getInstance("SHA-256")
            var byteCount = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        byteCount += count
                    }
                    output.fd.sync()
                }
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            AttachmentDownload(
                localFile = destination,
                mimeType = contentType,
                sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                byteCount = byteCount,
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        authenticated: Boolean = true,
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (authenticated) {
                val token = tokenStore.token() ?: throw SyncException(401, "This device is signed out.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) throw SyncException(status, json.optString("detail", "Request failed."))
            json
        } finally {
            connection.disconnect()
        }
    }
}

data class AttachmentDownload(
    val localFile: File? = null,
    val remoteUrl: String? = null,
    val mimeType: String? = null,
    val sha256: String? = null,
    val byteCount: Long? = null,
)

data class DataExport(val filename: String, val mimeType: String, val bytes: ByteArray)

internal fun exchangeUrl(baseUrl: String, kind: String, format: String, ids: List<String>): String {
    val cleanIds = ids.map(String::trim).filter(String::isNotBlank)
    val selection = if (cleanIds.isEmpty()) "" else "?ids=${URLEncoder.encode(cleanIds.joinToString(","), Charsets.UTF_8.name())}"
    return "$baseUrl/exchange/$kind.$format$selection"
}

class SyncException(val status: Int, override val message: String) : Exception(message)

internal fun resolveDownloadUrl(baseUrl: String, path: String): String = when {
    path.startsWith("http://") || path.startsWith("https://") -> path
    path.startsWith("/api/") -> baseUrl.substringBefore("/api/v1") + path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
