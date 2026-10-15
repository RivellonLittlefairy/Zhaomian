package com.example.passerwatch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.passerwatch.model.MonitorUiState
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun LensZoomControls(ui: MonitorUiState, onZoom: (Float) -> Unit) {
    val active = ui.lenses.firstOrNull { it.key == ui.activeLensKey }
    var displayedZoom by remember { mutableFloatStateOf(ui.zoomRatio) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(ui.zoomRatio, dragging) { if (!dragging) displayedZoom = ui.zoomRatio }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("镜头与缩放", style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.US, "%.2f×", if (dragging) displayedZoom else ui.zoomRatio), fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary)
            }
            Text("自动选镜 · ${active?.label ?: "正在读取镜头配置"}", style = MaterialTheme.typography.bodySmall)
            val adjustable = ui.maxZoom > ui.minZoom && ui.minZoom > 0
            if (adjustable) {
                // Logarithmic track keeps the useful low magnifications precise on 100x cameras.
                val span = ln(ui.maxZoom / ui.minZoom)
                val position = (ln(displayedZoom.coerceIn(ui.minZoom, ui.maxZoom) / ui.minZoom) / span).coerceIn(0f, 1f)
                Slider(value = position, onValueChange = { value ->
                    dragging = true
                    displayedZoom = ui.minZoom * (ui.maxZoom / ui.minZoom).pow(value)
                    onZoom(displayedZoom)
                }, onValueChangeFinished = { dragging = false; onZoom(displayedZoom) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(String.format(Locale.US, "%.2f×", ui.minZoom), style = MaterialTheme.typography.labelSmall)
                    Text(String.format(Locale.US, "%.2f×", ui.maxZoom), style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = (listOf(ui.minZoom) + ui.lenses.mapNotNull { it.baseZoom } + listOf(1f, 2f, 5f, 10f))
                        .filter { it in ui.minZoom..ui.maxZoom }.distinct().sorted()
                    presets.forEach { ratio ->
                        OutlinedButton(onClick = { displayedZoom = ratio; onZoom(ratio) }) {
                            Text(String.format(Locale.US, "%.1f×", ratio))
                        }
                    }
                }
            } else Text(if (ui.cameraReady) "当前镜头不支持可调缩放" else "相机就绪后可调整缩放",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}


