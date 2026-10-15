package com.example.passerwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passerwatch.model.MonitorUiState
import com.example.passerwatch.model.StorageState
import java.util.Locale

/** Pinch the preview to zoom; controls never cover the live scene. */
@Composable
fun LiveSentryPanel(
    ui: MonitorUiState,
    storage: StorageState,
    preview: @Composable () -> Unit,
    canStart: Boolean,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onOpenLatest: () -> Unit,
    onResumeSaving: () -> Unit,
    onZoom: (Float) -> Unit
) {
    val currentUi by rememberUpdatedState(ui)
    val updateZoom by rememberUpdatedState(onZoom)
    val colors = MaterialTheme.colorScheme
    val accent = when {
        ui.error != null || storage.paused -> MaterialTheme.colorScheme.error
        ui.running -> colors.primary
        else -> colors.tertiary
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(Modifier.fillMaxWidth(), color = colors.surfaceContainerLow, shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(accent))
                Text(ui.monitorText, modifier = Modifier.weight(1f), color = accent,
                    style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Column(horizontalAlignment = Alignment.End) {
                    Text("实际帧率", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(if (ui.running && ui.cameraReady && ui.actualFps > 0f) String.format(Locale.US, "%.1f FPS", ui.actualFps) else "— FPS",
                        style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
            }
        }
        // Camera and zoom label share stable screen bounds, independent of the lens output aspect.
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
            preview()
            // A Compose overlay owns the gesture, so AndroidView cannot swallow pinch events.
            // Accumulate locally for the whole touch sequence: camera zoom reports can lag while switching lenses.
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var requested = currentUi.zoomRatio
                    val enabled = currentUi.cameraReady
                    do {
                        val event = awaitPointerEvent()
                        if (enabled && event.changes.count { it.pressed && it.previousPressed } >= 2) {
                            val factor = event.calculateZoom()
                            val limits = currentUi
                            if (factor.isFinite() && factor > 0f && factor != 1f && limits.maxZoom > limits.minZoom) {
                                requested = (requested * factor).coerceIn(limits.minZoom, limits.maxZoom)
                                updateZoom(requested)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            })
        }
        if (ui.error != null || storage.paused) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(ui.error ?: storage.message, style = MaterialTheme.typography.bodySmall)
                    Row {
                        if (storage.paused) TextButton(onClick = onResumeSaving) { Text("恢复保存") }
                        if (ui.error != null && !ui.running) TextButton(onClick = onRetry) { Text("重试相机") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onToggle, enabled = ui.running || canStart,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ui.running) MaterialTheme.colorScheme.errorContainer else colors.primary,
                    contentColor = if (ui.running) MaterialTheme.colorScheme.onErrorContainer else colors.onPrimary)) {
                Text(if (ui.running) "■  停止检测" else "▶  开始检测")
            }
            val latest = storage.latest
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = latest != null, onClick = onOpenLatest)
                .padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (latest != null) Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) {
                    Column { LocalCaptureImage(latest.path, showMetadata = false) }
                }
                Column {
                    Text("最新抓拍 ›", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    Text(if (latest == null) "等待抓拍" else "已保存 ${ui.sessionId?.let { storage.savedCounts[it] } ?: 0} 张",
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
