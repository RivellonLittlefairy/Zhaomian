package com.example.passerwatch.capture

import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GalleryPublisherTest {
    @get:Rule val temp = TemporaryFolder()
    private class Entry(val failWrite: Boolean = false, val failPublish: Boolean = false) : GalleryEntry {
        val bytes = ByteArrayOutputStream()
        var closed = false
        var visible = false
        var discarded = false
        override fun open() = object : java.io.OutputStream() {
            override fun write(value: Int) { if (failWrite) throw IOException("write"); bytes.write(value) }
            override fun close() { closed = true }
        }
        override fun publish(): String {
            check(closed)
            if (failPublish) throw IOException("publish")
            visible = true; return "content://test/images/1"
        }
        override fun discard() { discarded = true; visible = false }
    }
    private fun jpeg() = temp.newFile("one.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    @Test fun successfulCopyPublishesOnlyAfterClosingStream() {
        val entry = Entry()
        val result = GalleryPublisher { entry }.publish(jpeg())
        assertEquals("content://test/images/1", result)
        assertArrayEquals(byteArrayOf(1, 2, 3), entry.bytes.toByteArray())
        assertTrue(entry.visible); assertFalse(entry.discarded)
    }
    @Test fun failedCopyDiscardsPendingEntry() {
        val entry = Entry(failWrite = true)
        assertThrows(IOException::class.java) { GalleryPublisher { entry }.publish(jpeg()) }
        assertTrue(entry.discarded); assertFalse(entry.visible)
    }
    @Test fun failedPublishDiscardsPendingEntry() {
        val entry = Entry(failPublish = true)
        assertThrows(IOException::class.java) { GalleryPublisher { entry }.publish(jpeg()) }
        assertTrue(entry.discarded); assertFalse(entry.visible)
    }
}
