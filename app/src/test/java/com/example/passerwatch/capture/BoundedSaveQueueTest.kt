package com.example.passerwatch.capture

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class BoundedSaveQueueTest {
    private class Image(val id: Int) : AutoCloseable {
        val released = AtomicInteger()
        override fun close() { released.incrementAndGet() }
    }
    @Test fun slowWriteDoesNotBlockAndFourthImageIsReleased() {
        val entered = CountDownLatch(1)
        val unblock = CountDownLatch(1)
        val done = CountDownLatch(3)
        val written = Collections.synchronizedList(mutableListOf<Int>())
        val images = (1..4).map { Image(it) }
        val queue = BoundedSaveQueue<Image, Int>({
            entered.countDown()
            assertTrue(unblock.await(5, TimeUnit.SECONDS))
            written.add(it.id)
            it.id
        }) { _, result -> assertTrue(result.isSuccess); done.countDown() }
        try {
            assertTrue(queue.submit(images[0]))
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertTrue(queue.submit(images[1]))
            assertTrue(queue.submit(images[2]))
            assertFalse(queue.submit(images[3]))
            assertEquals(1, images[3].released.get())
            queue.close() // closing stops new intake, accepted work still drains
            unblock.countDown()
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(1, 2, 3), written)
            images.forEach { assertEquals(1, it.released.get()) }
        } finally { unblock.countDown(); queue.close() }
    }
    @Test fun failedWriteDoesNotLoseLaterJobs() {
        val done = CountDownLatch(2)
        val outcomes = Collections.synchronizedList(mutableListOf<Boolean>())
        val queue = BoundedSaveQueue<Image, Int>({
            if (it.id == 1) error("disk failure")
            it.id
        }) { _, result -> outcomes.add(result.isSuccess); done.countDown() }
        try {
            assertTrue(queue.submit(Image(1)))
            assertTrue(queue.submit(Image(2)))
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(false, true), outcomes)
        } finally { queue.close() }
    }
    @Test fun closedQueueReleasesRejectedImage() {
        val queue = BoundedSaveQueue<Image, Int>({ it.id }) { _, _ -> }
        queue.close()
        val image = Image(1)
        assertFalse(queue.submit(image))
        assertEquals(1, image.released.get())
    }
}
