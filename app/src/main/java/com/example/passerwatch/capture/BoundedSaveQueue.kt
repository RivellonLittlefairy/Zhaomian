package com.example.passerwatch.capture

import java.util.ArrayDeque
import java.util.concurrent.Executors

class BoundedSaveQueue<T : AutoCloseable, R>(
    private val writer: (T) -> R,
    private val completed: (T, Result<R>) -> Unit
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { work -> Thread(work, "snapshot-io") }
    private val waiting = ArrayDeque<T>()
    private var busy = false
    private var closed = false

    @Synchronized fun submit(item: T): Boolean {
        if (closed || (busy && waiting.size >= 2)) {
            item.close()
            return false
        }
        if (busy) waiting.addLast(item)
        else {
            busy = true
            executor.execute { drain(item) }
        }
        return true
    }

    private fun drain(first: T) {
        var item: T? = first
        while (item != null) {
            val current = item
            val result = runCatching { writer(current) }
            runCatching { current.close() }
            // A broken observer must not strand the remaining owned images.
            runCatching { completed(current, result) }
            item = synchronized(this) {
                if (waiting.isEmpty()) { busy = false; null } else waiting.removeFirst()
            }
        }
    }

    @Synchronized override fun close() {
        closed = true
        executor.shutdown() // in-flight worker drains all accepted jobs
    }
}
