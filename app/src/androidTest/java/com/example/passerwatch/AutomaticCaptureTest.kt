package com.example.passerwatch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passerwatch.camera.OwnedFrame
import com.example.passerwatch.capture.CaptureRepository
import com.example.passerwatch.capture.SnapshotStore
import com.example.passerwatch.detection.PersonDetector
import com.example.passerwatch.model.PersonResult
import com.example.passerwatch.ui.MonitorViewModel
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomaticCaptureTest {
    @Test fun personDeparturePersonProducesTwoValidJpegsWithoutRestart() {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val app = instrument.targetContext.applicationContext as PasserWatchApplication
        val directory = File(app.cacheDir, "capture-test-${UUID.randomUUID()}")
        val repository = CaptureRepository(SnapshotStore(directory))
        val vm = MonitorViewModel(app, repository)
        val source = instrument.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }!!
        val detector = PersonDetector(app)
        try {
            waitUntil { vm.storage.value.initialized }
            vm.screenStarted(); vm.cameraReady(true); vm.start()
            val session = vm.sessionId()!!
            val result = detector.detect(source)
            assertTrue(result.hasPerson)
            fun frame(person: Boolean, at: Long, id: Long = session) {
                OwnedFrame(source.copy(Bitmap.Config.ARGB_8888, false), at).use {
                    vm.onFrame(id, it, PersonResult(person, result.inferenceMs))
                }
            }
            frame(true, 0); frame(true, 200)
            waitUntil { vm.storage.value.savedCounts[session] == 1 }
            for (time in 400L..2000L step 200) frame(true, time)
            assertEquals(1, vm.storage.value.savedCounts[session])
            for (time in 2200L..4200L step 200) frame(false, time)
            frame(true, 4400); frame(true, 4600)
            waitUntil { vm.storage.value.savedCounts[session] == 2 }
            assertTrue(vm.state.value.running)
            val files = directory.listFiles()!!.filter { it.extension == "jpg" }
            assertEquals(2, files.size)
            files.forEach { file ->
                val decoded = BitmapFactory.decodeFile(file.absolutePath)!!
                assertEquals(source.width, decoded.width)
                assertEquals(source.height, decoded.height)
                decoded.recycle()
            }
            vm.stop(); vm.start()
            val next = vm.sessionId()!!
            assertNotEquals(session, next)
            frame(true, 4800, session); frame(true, 5000, session)
            assertNull(vm.storage.value.savedCounts[next])
            frame(true, 5200, next); frame(true, 5400, next)
            waitUntil { vm.storage.value.savedCounts[next] == 1 }
        } finally { vm.screenStopped(); detector.close(); source.recycle() }
    }
    private fun waitUntil(check: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!check() && System.nanoTime() < end) Thread.sleep(20)
        assertTrue(check())
    }
}
