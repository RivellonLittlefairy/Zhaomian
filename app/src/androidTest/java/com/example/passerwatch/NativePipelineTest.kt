package com.example.passerwatch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passerwatch.detection.PersonDetector
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativePipelineTest {
    @Test fun stoppedSessionDoesNotStartModelsOrRecycleOriginalFrame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val frame = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        val detector = PersonDetector(context)
        try {
            assertThrows(java.util.concurrent.CancellationException::class.java) { detector.detect(frame) { false } }
            assertFalse(frame.isRecycled)
        } finally { detector.close(); frame.recycle() }
    }
    @Test fun detectorKeepsOriginalTriggerFrameUsable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val frame = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val detector = PersonDetector(context)
        try {
            assertFalse(detector.detect(frame).hasPerson)
            assertFalse("Inference must not recycle the full trigger frame", frame.isRecycled)
            frame.copy(Bitmap.Config.ARGB_8888, false).recycle()
        } finally { detector.close(); frame.recycle() }
    }
    @Test fun bundledModelRecognizesOfficialPersonSampleOffline() {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrument.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }!!
        val detector = PersonDetector(instrument.targetContext)
        try {
            val result = detector.detect(bitmap)
            assertTrue("Official portrait should be person", result.hasPerson)
            assertTrue("Visible face must qualify without full body", result.eligibleForCapture)
            assertFalse(bitmap.isRecycled)
        } finally { detector.close(); bitmap.recycle() }
    }
}
