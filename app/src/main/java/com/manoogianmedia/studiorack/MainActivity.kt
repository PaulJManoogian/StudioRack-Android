package com.manoogianmedia.studiorack

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.manoogianmedia.studiorack.data.EXTRA_NOTIFICATION_DESTINATION
import com.manoogianmedia.studiorack.data.EXTRA_NOTIFICATION_RECORD
import com.manoogianmedia.studiorack.data.EXTRA_NOTIFICATION_SOURCE
import com.manoogianmedia.studiorack.data.NotificationRoute
import com.manoogianmedia.studiorack.ui.StudioRackApp
import com.manoogianmedia.studiorack.ui.StudioRackViewModel
import com.manoogianmedia.studiorack.ui.StudioRackViewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val hardwareKeys = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private val notificationRoutes = MutableSharedFlow<NotificationRoute>(replay = 1, extraBufferCapacity = 1)
    private var gigModeActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        setContent {
            val repository = (application as StudioRackApplication).repository
            val model: StudioRackViewModel = viewModel(factory = StudioRackViewModelFactory(repository, (application as StudioRackApplication).localLive))
            StudioRackApp(model, hardwareKeys, notificationRoutes) { gigModeActive = it }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val sourceId = intent?.getStringExtra(EXTRA_NOTIFICATION_SOURCE).orEmpty()
        val destination = intent?.getStringExtra(EXTRA_NOTIFICATION_DESTINATION).orEmpty()
        val recordId = intent?.getStringExtra(EXTRA_NOTIFICATION_RECORD).orEmpty()
        if (sourceId.isNotBlank()) lifecycleScope.launch {
            (application as StudioRackApplication).repository.markNotificationRead(sourceId)
        }
        if (destination.isNotBlank()) notificationRoutes.tryEmit(NotificationRoute(destination, recordId))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isPedalKey = keyCode in setOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
        )
        if (gigModeActive && isPedalKey && event.repeatCount == 0) {
            hardwareKeys.tryEmit(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
