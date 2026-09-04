package com.manoogianmedia.studiorack.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StudioRackRepository(
    private val context: Context,
    private val dao: StudioRackDao,
    private val tokenStore: TokenStore,
    private val client: SyncClient,
) {
    fun signedIn() = tokenStore.isSignedIn()
    fun records(type: String): Flow<List<CachedRecord>> = dao.observeRecords(type)
    fun syncState(): Flow<SyncState?> = dao.observeSyncState()

    suspend fun signIn(email: String, accessCode: String, mfaCode: String) {
        val stableId = tokenStore.deviceId() ?: "android_" + Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val response = client.signIn(email, accessCode, mfaCode, "${Build.MANUFACTURER} ${Build.MODEL}", stableId)
        tokenStore.save(response.getString("access_token"), response.getString("device_id"), response.getString("account_id"))
        sync()
        scheduleAutomaticSync()
    }

    suspend fun signOut() {
        runCatching { client.revoke() }
        tokenStore.clear()
    }

    suspend fun sync() {
        pushPending()
        var cursor = dao.syncState()?.cursor ?: 0
        do {
            val response = client.pull(cursor)
            applyPull(response)
            cursor = response.getLong("cursor")
        } while (response.optBoolean("has_more", false))
    }

    fun syncNow() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
    }

    fun scheduleAutomaticSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("studiorack-sync", ExistingPeriodicWorkPolicy.KEEP, work)
    }

    private suspend fun pushPending() {
        val pending = dao.pending()
        if (pending.isEmpty()) return
        val payload = JSONArray()
        pending.forEach { mutation ->
            payload.put(
                JSONObject()
                    .put("mutation_id", mutation.mutationId)
                    .put("entity", mutation.entityType)
                    .put("id", mutation.entityId)
                    .put("operation", mutation.operation)
                    .put("base_revision", mutation.baseRevision)
                    .put("data", JSONObject(mutation.json))
            )
        }
        val results = client.push(payload).getJSONArray("results")
        for (index in 0 until results.length()) {
            val result = results.getJSONObject(index)
            val mutation = pending.first { it.mutationId == result.getString("mutation_id") }
            when (result.getString("status")) {
                "applied" -> dao.removeMutation(mutation.mutationId)
                "conflict" -> {
                    dao.putConflict(
                        SyncConflict(
                            mutation.mutationId,
                            mutation.entityType,
                            mutation.entityId,
                            mutation.json,
                            result.optJSONObject("server_data")?.toString(),
                            result.optInt("server_revision"),
                        )
                    )
                    dao.removeMutation(mutation.mutationId)
                }
            }
        }
    }

    private suspend fun applyPull(response: JSONObject) {
        val state = SyncState(
            cursor = response.getLong("cursor"),
            accountJson = response.getJSONObject("account").toString(),
            lastSyncAt = System.currentTimeMillis(),
        )
        val supporting = flattenSupporting(response.getJSONObject("supporting_entities"))
        if (response.optBoolean("full_snapshot")) {
            dao.replaceSnapshot(flattenEntities(response.getJSONObject("entities")), supporting, state)
            return
        }
        dao.clearSupporting()
        dao.putSupporting(supporting)
        val changes = response.getJSONArray("changes")
        for (index in 0 until changes.length()) {
            val change = changes.getJSONObject(index)
            val type = change.getString("entity")
            val id = change.getString("id")
            if (change.getString("operation") == "delete") dao.deleteRecord(type, id)
            else dao.putRecords(listOf(CachedRecord(type, id, change.getInt("revision"), change.getJSONObject("data").toString())))
        }
        dao.putState(state)
    }

    private fun flattenEntities(groups: JSONObject): List<CachedRecord> = buildList {
        groups.keys().forEach { type ->
            val rows = groups.getJSONArray(type)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(CachedRecord(type, row.getString("id"), row.optInt("revision"), row.getJSONObject("data").toString()))
            }
        }
    }

    private fun flattenSupporting(groups: JSONObject): List<SupportingRecord> = buildList {
        groups.keys().forEach { type ->
            val rows = groups.getJSONArray(type)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(SupportingRecord(type, row.getString("id"), row.getJSONObject("data").toString()))
            }
        }
    }
}
