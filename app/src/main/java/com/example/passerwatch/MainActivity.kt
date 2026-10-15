package com.example.passerwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passerwatch.ui.MonitorScreen
import com.example.passerwatch.ui.MonitorViewModel
import com.example.passerwatch.ui.theme.ZhaomianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MonitorViewModel = viewModel()
            val ui by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(ui.darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (ui.darkTheme) SystemBarStyle.dark(transparent)
                    else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            ZhaomianTheme(darkTheme = ui.darkTheme) { MonitorScreen(vm) }
        }
    }
}
