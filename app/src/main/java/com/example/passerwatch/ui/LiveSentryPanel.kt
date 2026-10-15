package com.example.passerwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.passerwatch.BuildConfig
import com.example.passerwatch.model.MonitorUiState
import com.example.passerwatch.model.StorageState
import com.example.passerwatch.ui.theme.*
import java.util.Locale

/** Camera stays visible and bound while the zoom sheet is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSentryPanel(
    ui: MonitorUiState,
    storage: StorageState,
    preview: @Composable () -> Unit,
    canStart: Boolean,
    manualEnabled: Boolean,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onOpenLatest: () -> Unit,
    onResumeSaving: () -> Unit,
    cameraControls: @Composable () -> Unit = {}
) {
    var showZoom by remember { mutableStateOf(false) }
    val accent = when {
        ui.error != null || storage.paused -> MaterialTheme.colorScheme.error
        ui.running -> SentryGreen
        else -> SentryAmber
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
            preview()
            Surface(Modifier.align(Alignment.TopStart).padding(10.dp),
                color = SentryLowest.copy(alpha = .78f), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(accent))
                    Text(ui.monitorText, color = accent, style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = { showZoom = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = SentryLowest.copy(alpha = .8f), contentColor = Color.White)) {
                Text(String.format(Locale.US, "%.1f×  缩放", ui.zoomRatio))
            }
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
                    containerColor = if (ui.running) MaterialTheme.colorScheme.errorContainer else SentryGreen,
                    contentColor = if (ui.running) MaterialTheme.colorScheme.onErrorContainer else SentryLowest)) {
                Text(if (ui.running) "■  停止检测" else "▶  开始检测")
            }
            val latest = storage.latest
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = latest != null, onClick = onOpenLatest)
                .padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (latest != null) Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) {
                    Column { LocalCaptureImage(latest.path, showMetadata = false) }
                }
                Column {
                    Text("最新抓拍 ›", style = MaterialTheme.typography.labelMedium, color = SentryGreen)
                    Text(if (latest == null) "等待抓拍" else "已保存 ${ui.sessionId?.let { storage.savedCounts[it] } ?: 0} 张",
                        style = MaterialTheme.typography.labelSmall, color = SentryMuted)
                }
            }
        }
    }
    if (showZoom) ModalBottomSheet(onDismissRequest = { showZoom = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 20.dp)) {
            cameraControls()
            if (BuildConfig.DEBUG) TextButton(onClick = onManual, enabled = manualEnabled) { Text("调试：手动拍照") }
        }
    }
}
