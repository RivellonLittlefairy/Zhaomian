package com.example.passerwatch.camera

import org.junit.Assert.*
import org.junit.Test

class ProcessingRateTest {
    @Test fun slowProcessingReportsFractionalFpsInsteadOfZero() {
        val rate = ProcessingRate()
        rate.record(0)
        assertEquals(0.8333f, rate.record(1200), 0.001f)
        assertEquals(0.8333f, rate.record(2400), 0.001f)
    }
    @Test fun regularTwentyFpsReportsTwenty() {
        val rate = ProcessingRate()
        (0L..950L step 50).forEach(rate::record)
        assertEquals(20f, rate.record(1000), 0.01f)
    }
    @Test fun newSessionDoesNotReuseOldMeasurements() {
        val rate = ProcessingRate()
        rate.record(0); rate.record(50)
        rate.reset()
        assertEquals(0f, rate.record(5000), 0f)
        assertEquals(20f, rate.record(5050), 0.01f)
    }
}
