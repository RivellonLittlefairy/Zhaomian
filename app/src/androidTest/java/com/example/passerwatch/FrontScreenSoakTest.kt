package com.example.passerwatch

import android.Manifest
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.passerwatch.ui.MonitorViewModel
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrontScreenSoakTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun thirtyMinutesOfRealCameraInference() {
        // Opt in explicitly; short regression runs must never reserve a phone for 30 minutes.
        assumeTrue(InstrumentationRegistry.getArguments().getString("soakMinutes") == "30")
        val instrument = InstrumentationRegistry.getInstrumentation()
        instrument.uiAutomation.grantRuntimePermission("com.example.passerwatch", Manifest.permission.CAMERA)
        compose.activityRule.scenario.recreate()
        lateinit var vm: MonitorViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[MonitorViewModel::class.java] }
        compose.waitUntil(15000) { vm.state.value.cameraReady && vm.storage.value.initialized }
        compose.runOnIdle { vm.start() }
        val started = SystemClock.elapsedRealtime()
        var minute = 0
        var lastFrames = 0
        val pss = mutableListOf<Int>()
        try {
            while (SystemClock.elapsedRealtime() - started < 30 * 60_000L) {
                Thread.sleep(1000)
                val state = vm.state.value
                assertTrue("Monitoring stopped: ${state.error}", state.running)
                assertNull(state.error)
                assertTrue(vm.storage.value.pending <= 3)
                val currentMinute = ((SystemClock.elapsedRealtime() - started) / 60_000).toInt()
                if (currentMinute > minute) {
                    minute = currentMinute
                    assertTrue("No inference progress in minute $minute", state.processedFrames > lastFrames)
                    lastFrames = state.processedFrames
                    val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
                    pss.add(memory.totalPss)
                    Log.i("ZhaomianSoak", "minute=$minute frames=$lastFrames inferenceMs=${state.inferenceMs} pssKb=${memory.totalPss} pending=${vm.storage.value.pending}")
                }
            }
            // After warm-up, reject obvious sustained growth, not ordinary GC fluctuation.
            assertTrue("Too few memory samples", pss.size >= 29)
            val baseline = pss.subList(2, 7).average()
            val tail = pss.takeLast(5).average()
            assertTrue("Significant PSS growth: $baseline -> $tail KB", tail <= baseline + 32 * 1024)
            assertTrue(vm.state.value.processedFrames > 1000)
        } finally { compose.runOnIdle { vm.stop() } }
    }
}
