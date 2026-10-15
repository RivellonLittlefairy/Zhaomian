package com.example.passerwatch.capture

import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AtomicSnapshotWriterTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun successfulEncodingPublishesOnlyFinalFile() {
        val writer = AtomicSnapshotWriter(temp.root) { 100_000_000 }
        val final = writer.write("one.jpg") { it.write(byteArrayOf(1, 2, 3)); true }
        assertArrayEquals(byteArrayOf(1, 2, 3), final.readBytes())
        assertEquals(listOf("one.jpg"), temp.root.list()!!.toList())
    }
    @Test fun falseCompressionCleansPartialFile() {
        val writer = AtomicSnapshotWriter(temp.root) { 100_000_000 }
        assertThrows(IOException::class.java) { writer.write("one.jpg") { it.write(1); false } }
        assertEquals(0, temp.root.list()!!.size)
    }
    @Test fun lowSpaceDoesNotAttemptEncoding() {
        var encoded = false
        val writer = AtomicSnapshotWriter(temp.root) { 0 }
        assertThrows(StorageFullException::class.java) { writer.write("one.jpg") { encoded = true; true } }
        assertFalse(encoded)
    }
    @Test fun failedEncodingCleansPartialFile() {
        val writer = AtomicSnapshotWriter(temp.root) { 100_000_000 }
        assertThrows(IOException::class.java) { writer.write("one.jpg") { throw IOException("write failed") } }
        assertEquals(0, temp.root.list()!!.size)
    }
    @Test fun latestIgnoresTemporaryFilesAndRespectsSaveOrder() {
        val old = temp.newFile("z.jpg").apply { writeBytes(byteArrayOf(1)); setLastModified(1000) }
        val latest = temp.newFile("a.jpg").apply { writeBytes(byteArrayOf(2)); setLastModified(2000) }
        temp.newFile("partial.tmp").setLastModified(3000)
        val writer = AtomicSnapshotWriter(temp.root)
        assertEquals(latest, writer.latest())
        writer.cleanTemporaryFiles()
        assertTrue(old.exists()); assertTrue(latest.exists())
        assertFalse(temp.root.resolve("partial.tmp").exists())
    }
}
