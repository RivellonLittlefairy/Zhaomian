package com.example.passerwatch.capture

import android.graphics.Bitmap
import android.system.ErrnoException
import android.system.OsConstants
import com.example.passerwatch.model.SnapshotRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SnapshotStore(val directory: File, private val gallery: GalleryPublisher? = null) : SnapshotStorage {
    private val writer = AtomicSnapshotWriter(directory)
    @Synchronized override fun initialize(): SnapshotRecord? {
        directory.mkdirs()
        writer.cleanTemporaryFiles()
        return latest()
    }
    @Synchronized override fun latest(): SnapshotRecord? = writer.latest()?.let {
        // Only query this app's unique filename; old private-only photos legitimately have no URI.
        val albumUri = runCatching { gallery?.findUri(it.name) }.getOrNull()
        SnapshotRecord(it.absolutePath, it.lastModified(), it.length(), albumUri)
    }
    override fun hasSpace(): Boolean = directory.usableSpace >= 4L * 1024 * 1024
    override fun galleryUri(record: SnapshotRecord): String? = record.galleryUri
        ?: gallery?.findUri(File(record.path).name)
    @Synchronized override fun list(): List<SnapshotRecord> = directory.listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.extension == "jpg" && it.length() > 0L }
        ?.map { SnapshotRecord(it.absolutePath, it.lastModified(), it.length()) }
        ?.sortedWith(compareByDescending<SnapshotRecord> { it.savedAt }.thenByDescending { it.path })
        ?.toList().orEmpty()

    @Synchronized override fun save(bitmap: Bitmap): SnapshotRecord {
        val now = System.currentTimeMillis()
        val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(now))
        try {
            val file = writer.write("capture_${time}_${UUID.randomUUID()}.jpg") {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
            val galleryUri = try { gallery?.publish(file) }
                catch (e: Exception) { file.delete(); throw e }
            return SnapshotRecord(file.absolutePath, file.lastModified(), file.length(), galleryUri)
        } catch (e: Exception) {
            var cause: Throwable? = e
            while (cause != null) {
                if (cause is ErrnoException && cause.errno == OsConstants.ENOSPC) throw StorageFullException()
                cause = cause.cause
            }
            if (!hasSpace() || e.message?.contains("ENOSPC") == true ||
                e.message?.contains("No space left") == true) throw StorageFullException()
            throw e
        }
    }
}
