package com.manoogianmedia.studiorack

import android.app.Application
import android.content.ComponentCallbacks2
import com.manoogianmedia.studiorack.data.StudioRackDatabase
import com.manoogianmedia.studiorack.data.StudioRackRepository
import com.manoogianmedia.studiorack.data.SyncClient
import com.manoogianmedia.studiorack.data.TokenStore
import com.manoogianmedia.studiorack.data.LocalLiveCoordinator
import com.manoogianmedia.studiorack.ui.clearPerformanceAttachmentMemoryCache

class StudioRackApplication : Application() {
    val repository: StudioRackRepository by lazy {
        val tokenStore = TokenStore(this)
        StudioRackRepository(
            this,
            StudioRackDatabase.create(this).dao(),
            tokenStore,
            SyncClient(tokenStore, getString(R.string.api_base_url), getString(R.string.export_file_prefix)),
        )
    }
    val localLive: LocalLiveCoordinator by lazy { LocalLiveCoordinator(this) }

    override fun onCreate() {
        super.onCreate()
        repository.createNotificationChannels()
        if (repository.signedIn()) repository.scheduleAutomaticSync()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clearPerformanceAttachmentMemoryCache()
        }
    }
}
