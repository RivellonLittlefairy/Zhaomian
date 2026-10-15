package com.example.passerwatch.monitor

import java.util.concurrent.atomic.AtomicLong

class SessionTracker {
    private var current = 0L
    private var running = false
    @Synchronized fun start(): Long { running = true; current = sequence.incrementAndGet(); return current }
    @Synchronized fun stop() { running = false }
    @Synchronized fun accepts(id: Long): Boolean = running && current == id
    companion object { private val sequence = AtomicLong() }
}
