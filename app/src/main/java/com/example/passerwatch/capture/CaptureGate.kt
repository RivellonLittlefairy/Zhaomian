package com.example.passerwatch.capture

class CaptureGate(
    private val confirmationFrames: Int = 2,
    private val vacantMs: Long = 2000,
    private val maxResultGapMs: Long = 1000
) {
    init {
        require(confirmationFrames in 1..5)
        require(vacantMs in 1000L..5000L)
        require(maxResultGapMs >= 1000L)
    }
    var waitingForDeparture: Boolean = false
        private set
    private var positiveCount = 0
    private var lastResult: Long? = null
    private var absentSince: Long? = null

    fun reset() {
        waitingForDeparture = false
        positiveCount = 0
        lastResult = null
        absentSince = null
    }
    fun interrupt() { positiveCount = 0; lastResult = null; absentSince = null }

    // null is failed inference, never evidence of an empty scene.
    fun accept(person: Boolean?, elapsedMs: Long, eligible: Boolean = person == true): Boolean {
        val previous = lastResult
        if (previous != null && (elapsedMs - previous > maxResultGapMs || elapsedMs < previous)) {
            positiveCount = 0
            absentSince = null
        }
        lastResult = elapsedMs
        if (person == null) {
            positiveCount = 0
            absentSince = null
            return false
        }
        if (waitingForDeparture) {
            if (person) absentSince = null
            else {
                val since = absentSince ?: elapsedMs.also { absentSince = it }
                if (elapsedMs - since >= vacantMs) {
                    waitingForDeparture = false
                    positiveCount = 0
                    absentSince = null
                }
            }
            return false
        }
        positiveCount = if (person && eligible) positiveCount + 1 else 0
        if (positiveCount < confirmationFrames) return false
        waitingForDeparture = true
        positiveCount = 0
        absentSince = null
        return true
    }
}
