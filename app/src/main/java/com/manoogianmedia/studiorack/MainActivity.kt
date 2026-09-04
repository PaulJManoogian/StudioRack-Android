package com.manoogianmedia.studiorack

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manoogianmedia.studiorack.ui.StudioRackApp
import com.manoogianmedia.studiorack.ui.StudioRackViewModel
import com.manoogianmedia.studiorack.ui.StudioRackViewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val hardwareKeys = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private var gigModeActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repository = (application as StudioRackApplication).repository
            val model: StudioRackViewModel = viewModel(factory = StudioRackViewModelFactory(repository))
            StudioRackApp(model, hardwareKeys) { gigModeActive = it }
        }
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
