package com.example.passerwatch

import android.app.Application
import com.example.passerwatch.capture.CaptureRepository
import com.example.passerwatch.capture.SnapshotStore
import com.example.passerwatch.capture.GalleryPublisher
import com.example.passerwatch.capture.MediaStoreGallerySink
import java.io.File

class PasserWatchApplication : Application() {
    val captures by lazy { CaptureRepository(SnapshotStore(File(filesDir, "captures"), GalleryPublisher(MediaStoreGallerySink(this)))) }
}
