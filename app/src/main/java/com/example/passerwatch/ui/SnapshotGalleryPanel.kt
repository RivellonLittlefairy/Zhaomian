package com.example.passerwatch.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.passerwatch.model.SnapshotRecord
import com.example.passerwatch.model.StorageState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A lazy, real-file gallery. The surrounding screen owns navigation and the image viewer. */
@Composable
fun SnapshotGalleryPanel(
    storage: StorageState,
    records: List<SnapshotRecord>,
    onRefresh: () -> Unit,
    onOpen: (SnapshotRecord) -> Unit
) {
    val context = LocalContext.current
    var todayOnly by rememberSaveable { mutableStateOf(false) }
    var copied by rememberSaveable { mutableStateOf(false) }
    val directory = remember(context) { File(context.filesDir, "captures").absolutePath }
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = startOfToday
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val today = remember(records, startOfToday) {
        records.filter { it.savedAt in startOfToday until tomorrow }
    }
    val visible = if (todayOnly) today else records
    val bytes = remember(records) { records.sumOf { it.bytes } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "storage") {
            GalleryCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("抓拍存储", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("LOCAL SNAPSHOTS", style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(if (storage.paused) "保存已暂停" else if (!storage.initialized) "读取中" else "本地存储",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (storage.paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${records.size} 张图片", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                    Text(formatGalleryBytes(bytes), color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                }
                Text("系统相册 Pictures/Zhaomian + App 内副本",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("旧的私有照片可能没有系统相册副本；列表以 App 内实际文件为准。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(storage.message, style = MaterialTheme.typography.bodySmall,
                    color = if (storage.paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                if (storage.pending > 0) Text("写入队列：${storage.pending} 张等待保存",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !todayOnly, onClick = { todayOnly = false },
                        label = { Text("全部 (${records.size})") })
                    FilterChip(selected = todayOnly, onClick = { todayOnly = true },
                        label = { Text("今天 (${today.size})") })
                }
            }
        }
        item(key = "heading") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("本地快照画廊", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("按保存时间倒序 · ${visible.size} 张", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onRefresh) { Text("刷新") }
            }
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                GalleryCard {
                    Text(if (!storage.initialized) "正在读取抓拍…" else if (todayOnly) "今天还没有抓拍" else "等待第一张抓拍",
                        style = MaterialTheme.typography.titleMedium)
                    Text("开始巡视后，确认有人出现时会自动保存照片。人在场期间只保存一次。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(visible, key = { it.path }) { record ->
            SnapshotGalleryRecord(record, onOpen)
        }
        item(key = "directory") {
            GalleryCard {
                Text("本地文件目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("APP PRIVATE COPY", style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                Text(directory, style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("App 内副本会随卸载删除；已发布到系统相册的照片可在相册中管理。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Zhaomian 本地抓拍目录", directory))
                    copied = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (copied) "已复制本地路径 · 再次复制" else "复制本地目录路径")
                }
            }
        }
    }
}

@Composable
private fun SnapshotGalleryRecord(record: SnapshotRecord, onOpen: (SnapshotRecord) -> Unit) {
    val timestamp = remember(record.savedAt) {
        SimpleDateFormat("yyyy.MM.dd  HH:mm:ss", Locale.getDefault()).format(Date(record.savedAt))
    }
    Card(shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(timestamp, style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text("JPEG", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary)
        }
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 8.dp, horizontal = 12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LocalCaptureImage(record.path)
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(File(record.path).name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${formatGalleryBytes(record.bytes)} · App 内原始抓拍",
                style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onOpen(record) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)) { Text("查看原图") }
        }
    }
}

@Composable
private fun GalleryCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

private fun formatGalleryBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
