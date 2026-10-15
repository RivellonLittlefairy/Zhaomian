package com.example.passerwatch.capture

import org.junit.Assert.*
import org.junit.Test

class CaptureGateTest {
    @Test fun partialBodyWithoutFaceDoesNotTrigger() {
        val gate = CaptureGate()
        assertFalse(gate.accept(true, 0, eligible = false))
        assertFalse(gate.accept(true, 200, eligible = false))
        assertFalse(gate.accept(true, 400, eligible = true))
        assertTrue(gate.accept(true, 600, eligible = true))
    }
    @Test fun losingFaceWhilePersonStaysDoesNotRearm() {
        val gate = triggered()
        (400L..4000L step 200).forEach { gate.accept(true, it, eligible = false) }
        assertTrue(gate.waitingForDeparture)
        assertFalse(gate.accept(true, 4200, eligible = true))
    }
    @Test fun requiresTwoConsecutivePositiveFrames() {
        val gate = CaptureGate()
        assertFalse(gate.accept(true, 0))
        assertTrue(gate.accept(true, 200))
        assertTrue(gate.waitingForDeparture)
    }
    @Test fun emptyFrameResetsConfirmation() {
        val gate = CaptureGate()
        gate.accept(true, 0)
        assertFalse(gate.accept(false, 200))
        assertFalse(gate.accept(true, 400))
        assertTrue(gate.accept(true, 600))
    }
    @Test fun longResultGapDoesNotConfirmAppearance() {
        val gate = CaptureGate()
        gate.accept(true, 0)
        assertFalse(gate.accept(true, 1001))
        assertTrue(gate.accept(true, 1201))
    }
    @Test fun stayDoesNotTriggerAgain() {
        val gate = CaptureGate()
        gate.accept(true, 0)
        gate.accept(true, 200)
        (400L..6000L step 200).forEach { assertFalse(gate.accept(true, it)) }
        assertTrue(gate.waitingForDeparture)
    }
    @Test fun shortAbsenceDoesNotRearm() {
        val gate = triggered()
        gate.accept(false, 400)
        gate.accept(false, 1000)
        assertFalse(gate.accept(true, 1200))
        assertTrue(gate.waitingForDeparture)
    }
    @Test fun twoSecondsOfValidAbsenceAllowsNextAppearance() {
        val gate = triggered()
        (400L..2400L step 200).forEach { assertFalse(gate.accept(false, it)) }
        assertFalse(gate.waitingForDeparture)
        assertFalse(gate.accept(true, 2600))
        assertTrue(gate.accept(true, 2800))
    }
    @Test fun missingResultsAreNotDepartureEvidence() {
        val gate = triggered()
        gate.accept(false, 400)
        gate.accept(false, 3000)
        assertTrue(gate.waitingForDeparture)
        gate.accept(false, 3200)
        gate.accept(null, 3400)
        gate.accept(false, 3600)
        gate.accept(false, 4400)
        assertTrue(gate.waitingForDeparture)
    }
    @Test fun failedDetectionResetsAppearance() {
        val gate = CaptureGate()
        gate.accept(true, 0)
        gate.accept(null, 200)
        assertFalse(gate.accept(true, 400))
        assertTrue(gate.accept(true, 600))
    }
    @Test fun newSessionResetsAllEvidence() {
        val gate = triggered()
        gate.reset()
        assertFalse(gate.waitingForDeparture)
        assertFalse(gate.accept(true, 400))
        assertTrue(gate.accept(true, 600))
    }
    private fun triggered() = CaptureGate().apply { accept(true, 0); accept(true, 200) }
}
