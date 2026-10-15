package com.example.passerwatch

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.passerwatch.capture.CaptureRepository
import com.example.passerwatch.capture.SnapshotStorage
import com.example.passerwatch.capture.StorageFullException
import com.example.passerwatch.model.SnapshotRecord
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryConcurrencyTest {
    private open class Storage : SnapshotStorage {
        val writes = AtomicInteger()
        @Volatile var lowSpace = false
        override fun initialize(): SnapshotRecord? = null
        override fun latest(): SnapshotRecord? = null
        override fun hasSpace() = !lowSpace
        override fun save(bitmap: Bitmap): SnapshotRecord {
            writes.incrementAndGet()
            if (lowSpace) throw StorageFullException()
            return SnapshotRecord("new.jpg", 2000, 100)
        }
    }
    @Test fun normalReloadCannotEnableWritesBeforeInitialCleanupFinishes() {
        val entered = CountDownLatch(1)
        val unblock = CountDownLatch(1)
        val ordinary = CountDownLatch(1)
        val storage = object : Storage() {
            override fun initialize(): SnapshotRecord? { entered.countDown(); unblock.await(5, TimeUnit.SECONDS); return null }
            override fun latest(): SnapshotRecord? { ordinary.countDown(); return null }
        }
        val repo = CaptureRepository(storage)
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            repo.reloadLatest()
            // Give ordinary reload a chance to run; it must wait on initialization.
            ordinary.await(400, TimeUnit.MILLISECONDS)
            assertFalse(repo.state.value.initialized)
            val bitmap = image()
            assertFalse(repo.submit(bitmap, 1))
            assertTrue(bitmap.isRecycled)
            assertEquals(0, storage.writes.get())
        } finally { unblock.countDown() }
        waitUntil { repo.state.value.initialized }
    }
    @Test fun staleScanCannotOverwriteConcurrentlySavedLatest() {
        val entered = CountDownLatch(1)
        val unblock = CountDownLatch(1)
        val storage = object : Storage() {
            override fun latest(): SnapshotRecord? {
                entered.countDown(); unblock.await(5, TimeUnit.SECONDS)
                return SnapshotRecord("old.jpg", 1000, 100)
            }
        }
        val repo = CaptureRepository(storage)
        waitUntil { repo.state.value.initialized }
        repo.reloadLatest()
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertTrue(repo.submit(image(), 1))
        waitUntil { repo.state.value.latest?.path == "new.jpg" }
        unblock.countDown()
        Thread.sleep(200)
        assertEquals("new.jpg", repo.state.value.latest?.path)
    }
    @Test fun fullStoragePausesAndResumesWithoutRetryingSameImage() {
        val storage = Storage().apply { lowSpace = true }
        val repo = CaptureRepository(storage)
        waitUntil { repo.state.value.initialized }
        assertTrue(repo.submit(image(), 1))
        waitUntil { repo.state.value.paused }
        val dropped = image()
        assertFalse(repo.submit(dropped, 1))
        assertTrue(dropped.isRecycled)
        assertEquals(1, storage.writes.get())
        storage.lowSpace = false
        repo.resumeSaving()
        waitUntil { !repo.state.value.paused }
        assertTrue(repo.submit(image(), 2))
        waitUntil { repo.state.value.savedCounts[2] == 1 }
        assertEquals(2, storage.writes.get())
    }
    private fun image() = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    private fun waitUntil(check: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!check() && System.nanoTime() < end) Thread.sleep(20)
        assertTrue(check())
    }
}
