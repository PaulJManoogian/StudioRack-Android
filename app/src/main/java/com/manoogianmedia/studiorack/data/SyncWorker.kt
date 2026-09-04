package com.manoogianmedia.studiorack.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manoogianmedia.studiorack.StudioRackApplication

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as StudioRackApplication).repository
        if (!repository.signedIn()) return Result.success()
        return try {
            repository.sync()
            Result.success()
        } catch (error: SyncException) {
            if (error.status == 401) Result.failure() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

