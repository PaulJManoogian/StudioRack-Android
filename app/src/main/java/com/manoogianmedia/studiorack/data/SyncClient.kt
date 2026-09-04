package com.manoogianmedia.studiorack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class SyncClient(
    private val tokenStore: TokenStore,
    private val baseUrl: String = "https://www.manoogianmedia.com/studiorack/api/v1",
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

    suspend fun revoke() = request("/auth/revoke", "POST", JSONObject())

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
            if (status !in 200..299) throw SyncException(status, json.optString("detail", "StudioRack request failed."))
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

class SyncException(val status: Int, override val message: String) : Exception(message)

internal fun resolveDownloadUrl(baseUrl: String, path: String): String = when {
    path.startsWith("http://") || path.startsWith("https://") -> path
    path.startsWith("/api/") -> baseUrl.substringBefore("/api/v1") + path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
