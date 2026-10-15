package com.example.passerwatch.camera

import org.junit.Assert.*
import org.junit.Test

class FrameSamplerTest {
    @Test fun limitsHighRateCameraToTwentyFramesPerSecond() {
        val sampler = FrameSampler()
        assertEquals(20, (0L until 1000L step 10).count { sampler.accept(it) })
    }
    @Test fun thirtyFpsCameraCanReachTwentyInsteadOfDroppingToFifteen() {
        val sampler = FrameSampler()
        assertEquals(20, (0L until 30L).count { sampler.accept(it * 1000 / 30) })
    }
    @Test fun longPauseDoesNotAccumulateCatchUpWork() {
        val sampler = FrameSampler()
        assertTrue(sampler.accept(0))
        assertTrue(sampler.accept(5000))
        assertFalse(sampler.accept(5020))
        assertTrue(sampler.accept(5050))
    }
    @Test fun nextSessionStartsImmediatelyAfterReset() {
        val sampler = FrameSampler()
        assertTrue(sampler.accept(0))
        assertFalse(sampler.accept(20))
        sampler.reset()
        assertTrue(sampler.accept(21))
    }
}
