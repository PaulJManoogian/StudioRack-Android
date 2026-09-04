package com.manoogianmedia.studiorack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

class SyncException(val status: Int, override val message: String) : Exception(message)

