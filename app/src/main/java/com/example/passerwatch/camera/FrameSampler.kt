package com.example.passerwatch.camera

class FrameSampler(targetFps: Int = 20) {
    init { require(targetFps in 1..30) }
    private val intervalMs = 1000.0 / targetFps
    private var nextDue: Double? = null
    private var lastObserved: Long? = null
    fun accept(elapsedMs: Long): Boolean {
        if (lastObserved?.let { elapsedMs < it } == true) reset()
        lastObserved = elapsedMs
        val due = nextDue ?: elapsedMs.toDouble()
        if (elapsedMs < due) return false
        // Advance scheduled deadlines instead of insisting on 50ms since the last camera callback.
        // A 30 FPS camera then alternates 33/67ms gaps and reaches ~20 FPS instead of 15.
        nextDue = if (elapsedMs - due >= intervalMs) elapsedMs + intervalMs else due + intervalMs
        return true
    }
    fun reset() { nextDue = null; lastObserved = null }
}
