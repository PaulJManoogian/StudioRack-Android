package com.manoogianmedia.studiorack

import android.app.Application
import com.manoogianmedia.studiorack.data.StudioRackDatabase
import com.manoogianmedia.studiorack.data.StudioRackRepository
import com.manoogianmedia.studiorack.data.SyncClient
import com.manoogianmedia.studiorack.data.TokenStore

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

    override fun onCreate() {
        super.onCreate()
        repository.createNotificationChannels()
        if (repository.signedIn()) repository.scheduleAutomaticSync()
    }
}
