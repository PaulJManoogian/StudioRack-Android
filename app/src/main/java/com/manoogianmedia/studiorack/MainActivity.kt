package com.manoogianmedia.studiorack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manoogianmedia.studiorack.ui.StudioRackApp
import com.manoogianmedia.studiorack.ui.StudioRackViewModel
import com.manoogianmedia.studiorack.ui.StudioRackViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repository = (application as StudioRackApplication).repository
            val model: StudioRackViewModel = viewModel(factory = StudioRackViewModelFactory(repository))
            StudioRackApp(model)
        }
    }
}

