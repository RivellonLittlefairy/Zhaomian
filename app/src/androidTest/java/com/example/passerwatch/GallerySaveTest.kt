package com.example.passerwatch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passerwatch.capture.GalleryPublisher
import com.example.passerwatch.capture.MediaStoreGallerySink
import com.example.passerwatch.capture.SnapshotStore
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GallerySaveTest {
    @Test fun jpegIsPublishedToAlbumAndCanBeDecodedWithoutReadPermission() {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "gallery-test-${UUID.randomUUID()}")
        val store = SnapshotStore(directory, GalleryPublisher(MediaStoreGallerySink(context)))
        store.initialize()
        val image = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        var uri: Uri? = null
        var privateFile: File? = null
        try {
            val record = store.save(image)
            uri = Uri.parse(record.galleryUri!!)
            privateFile = File(record.path)
            assertTrue(privateFile.isFile)
            // A fresh scan after returning from the gallery/process restart must retain album access.
            assertEquals(uri.lastPathSegment, Uri.parse(store.latest()?.galleryUri!!).lastPathSegment)
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.IS_PENDING,
                MediaStore.Images.Media.RELATIVE_PATH), null, null, null)!!.use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
                assertEquals("Pictures/PasserWatch/", it.getString(1))
            }
            context.contentResolver.openInputStream(uri)!!.use { input ->
                val decoded = BitmapFactory.decodeStream(input)!!
                assertEquals(64, decoded.width); assertEquals(48, decoded.height)
                decoded.recycle()
            }
        } finally {
            image.recycle()
            uri?.let { context.contentResolver.delete(it, null, null) }
            privateFile?.delete()
            directory.delete()
        }
    }
}
