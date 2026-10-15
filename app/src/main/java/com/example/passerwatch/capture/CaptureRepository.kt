package com.example.passerwatch.capture

import android.graphics.Bitmap
import android.util.Log
import com.example.passerwatch.model.StorageState
import com.example.passerwatch.model.SnapshotRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class SaveJob(val bitmap: Bitmap, val sessionId: Long?) : AutoCloseable {
    override fun close() { bitmap.recycle() }
}

// Application-owned finite jobs survive a screen disappearing. No Activity/View is held here.
class CaptureRepository(private val store: SnapshotStorage) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(StorageState())
    val state = mutable.asStateFlow()
    private val mutableRecords = MutableStateFlow<List<SnapshotRecord>>(emptyList())
    val records = mutableRecords.asStateFlow()
    private val queue = BoundedSaveQueue<SaveJob, com.example.passerwatch.model.SnapshotRecord>(
        writer = { job ->
            if (mutable.value.paused) throw StorageFullException()
            store.save(job.bitmap)
        },
        completed = { job, result ->
            mutable.update { old ->
                result.fold(
                    onSuccess = { record ->
                        val counts = old.savedCounts.toMutableMap()
                        job.sessionId?.let { counts[it] = (counts[it] ?: 0) + 1 }
                        while (counts.size > 8) counts.remove(counts.keys.first())
                        old.copy(latest = record, pending = (old.pending - 1).coerceAtLeast(0),
                            message = if (record.galleryUri != null) "已保存到相册 · Pictures/PasserWatch" else "已保存到 App 内", savedCounts = counts)
                    },
                    onFailure = { error ->
                        old.copy(pending = (old.pending - 1).coerceAtLeast(0),
                            paused = old.paused || error is StorageFullException,
                            message = if (error is StorageFullException) "存储不足，保存已暂停；检测继续"
                                else "保存失败：${error.message ?: "未知错误"}")
                    }
                )
            }
            result.onSuccess {
                Log.i("PasserWatch", "saved session=${job.sessionId} bytes=${it.bytes}")
                runCatching { refreshRecords() }.onFailure { error ->
                    Log.w("PasserWatch", "Unable to refresh capture list", error)
                }
            }
        }
    )
    private val initialization = scope.launch { reload(initial = true) }

    // Ownership is transferred even when intake is rejected.
    @Synchronized fun submit(bitmap: Bitmap, sessionId: Long?): Boolean {
        val job = SaveJob(bitmap, sessionId)
        if (!mutable.value.initialized || mutable.value.paused) { job.close(); return false }
        mutable.update { it.copy(pending = it.pending + 1, message = "正在保存…") }
        val accepted = queue.submit(job)
        if (!accepted) mutable.update { it.copy(pending = (it.pending - 1).coerceAtLeast(0), message = "保存过载：本次图片未保存") }
        return accepted
    }

    fun resumeSaving() { scope.launch {
        val enough = store.hasSpace()
        mutable.update { it.copy(paused = !enough, message = if (enough)
            "保存已恢复，等待下一次出现；错过的图片不补存" else "空间仍不足，请释放空间后再试") }
    } }

    fun reloadLatest() { scope.launch { initialization.join(); reload(initial = false) } }
    suspend fun resolveGalleryUri(record: SnapshotRecord): String? = withContext(Dispatchers.IO) {
        store.galleryUri(record)
    }
    private fun reload(initial: Boolean) {
        val observedLatest = mutable.value.latest
        try {
            // Temporary-file cleanup is only allowed before the very first intake.
            val record = if (initial) store.initialize() else store.latest()
            mutable.update { it.copy(initialized = it.initialized || initial,
                latest = if (it.latest == observedLatest) record ?: it.latest else it.latest,
                message = if (initial) "新抓拍保存到相册 · Pictures/PasserWatch" else it.message) }
            refreshRecords()
        } catch (e: Exception) {
            mutable.update { it.copy(initialized = it.initialized || initial, message = "读取图片失败：${e.message}") }
        }
    }

    // Serialize scans so an older refresh cannot overwrite the list after a newer save.
    @Synchronized private fun refreshRecords() {
        val scanned = store.list()
        val newest = mutable.value.latest
        mutableRecords.value = scanned.map { record ->
            if (record.path == newest?.path) record.copy(galleryUri = newest?.galleryUri) else record
        }
    }
}
