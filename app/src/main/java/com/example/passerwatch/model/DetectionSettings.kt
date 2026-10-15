package com.example.passerwatch.model

/** Applied only between monitoring sessions, when the analysis pipeline is rebuilt. */
data class DetectionSettings(
    val targetFps: Int = 20,
    val scoreThreshold: Float = 0.6f,
    val confirmationFrames: Int = 2,
    val vacantSeconds: Float = 2f
) {
    init {
        require(targetFps in 1..30)
        require(scoreThreshold.isFinite() && scoreThreshold in 0.3f..0.95f)
        require(confirmationFrames in 1..5)
        require(vacantSeconds.isFinite() && vacantSeconds in 1f..5f)
    }

    val vacantMs: Long get() = (vacantSeconds * 1000).toLong()
    // At 1 FPS, allow CameraX callback jitter without resetting every valid sample.
    val maxResultGapMs: Long get() = maxOf(1000L, kotlin.math.ceil(1000.0 / targetFps).toLong() + 250L)
}
