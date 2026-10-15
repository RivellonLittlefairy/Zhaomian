package com.example.passerwatch.camera

import java.util.ArrayDeque

class ProcessingRate {
    private val recent = ArrayDeque<Long>()
    fun record(elapsedMs: Long): Float {
        if (recent.lastOrNull()?.let { elapsedMs < it } == true) recent.clear()
        recent.addLast(elapsedMs)
        // Keep one previous completion even below 1 FPS, so slow inference is not displayed as 0.
        while (recent.size > 2 && elapsedMs - recent.first > 1000) recent.removeFirst()
        val span = elapsedMs - recent.first
        return if (span > 0) (recent.size - 1) * 1000f / span else 0f
    }
    fun reset() { recent.clear() }
}
