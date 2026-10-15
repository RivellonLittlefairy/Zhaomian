package com.example.passerwatch.capture

import android.graphics.Bitmap
import com.example.passerwatch.model.SnapshotRecord

interface SnapshotStorage {
    fun initialize(): SnapshotRecord?
    fun latest(): SnapshotRecord?
    fun list(): List<SnapshotRecord> = listOfNotNull(latest())
    fun galleryUri(record: SnapshotRecord): String? = record.galleryUri
    fun hasSpace(): Boolean
    fun save(bitmap: Bitmap): SnapshotRecord
}
