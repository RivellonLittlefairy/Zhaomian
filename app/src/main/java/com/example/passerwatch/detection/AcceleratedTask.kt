package com.example.passerwatch.detection

import java.util.concurrent.CancellationException

enum class InferenceBackend { GPU, CPU }

class AcceleratedTask<T : AutoCloseable>(
    private val preferGpu: Boolean = true,
    private val onFallback: (Exception) -> Unit = {},
    private val factory: (InferenceBackend) -> T
) : AutoCloseable {
    var backend: InferenceBackend? = null
        private set
    var fellBack = false
        private set
    private var task: T? = null
    private var closed = false

    private fun checkpoint(stillActive: () -> Boolean) {
        if (!stillActive()) throw CancellationException("Monitoring stopped")
    }
    private fun create(stillActive: () -> Boolean): T {
        checkpoint(stillActive)
        if (preferGpu && !fellBack) {
            try {
                return factory(InferenceBackend.GPU).also { backend = InferenceBackend.GPU; task = it }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fellBack = true; onFallback(e) }
        }
        checkpoint(stillActive)
        return factory(InferenceBackend.CPU).also { backend = InferenceBackend.CPU; task = it }
    }

    fun <R> run(stillActive: () -> Boolean = { true }, operation: (T) -> R): R {
        check(!closed)
        checkpoint(stillActive)
        val engine = task ?: create(stillActive)
        try { checkpoint(stillActive); return operation(engine) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (backend != InferenceBackend.GPU) throw e
            task = null
            runCatching { engine.close() }
            fellBack = true
            onFallback(e)
            val cpu = create(stillActive)
            checkpoint(stillActive)
            return operation(cpu)
        }
    }
    override fun close() { closed = true; val old = task; task = null; old?.close() }
}
