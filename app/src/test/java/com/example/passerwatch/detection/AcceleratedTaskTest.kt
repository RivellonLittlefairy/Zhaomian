package com.example.passerwatch.detection

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class AcceleratedTaskTest {
    @Test fun stoppingDuringFailedGpuInitializationDoesNotCreateCpuModel() {
        var active = true
        val attempts = mutableListOf<InferenceBackend>()
        val task = AcceleratedTask<Engine> { backend ->
            attempts.add(backend)
            active = false
            error("GPU initialization failed after stop")
        }
        assertThrows(CancellationException::class.java) { task.run(stillActive = { active }) { it.backend } }
        assertEquals(listOf(InferenceBackend.GPU), attempts)
        task.close()
    }
    @Test fun stoppingDuringFailedGpuInferenceReleasesGpuWithoutCreatingCpu() {
        var active = true
        val created = mutableListOf<Engine>()
        val task = AcceleratedTask { Engine(it).also(created::add) }
        assertThrows(CancellationException::class.java) {
            task.run(stillActive = { active }) { active = false; error("GPU inference failed after stop") }
        }
        assertEquals(1, created.size)
        assertTrue(created.single().closed)
        task.close()
    }
    private class Engine(val backend: InferenceBackend) : AutoCloseable {
        var closed = false
        override fun close() { check(!closed); closed = true }
    }
    @Test fun triesGpuAndReusesItUntilClosed() {
        val created = mutableListOf<Engine>()
        val task = AcceleratedTask { Engine(it).also(created::add) }
        assertEquals(InferenceBackend.GPU, task.run { it.backend })
        task.run { it.backend }
        assertEquals(1, created.size)
        assertEquals(InferenceBackend.GPU, task.backend)
        task.close()
        assertTrue(created.single().closed)
    }
    @Test fun initializationFailureFallsBackOnce() {
        val attempts = mutableListOf<InferenceBackend>()
        val task = AcceleratedTask { backend ->
            attempts.add(backend)
            if (backend == InferenceBackend.GPU) error("unsupported GPU")
            Engine(backend)
        }
        assertEquals(InferenceBackend.CPU, task.run { it.backend })
        task.run { it.backend }
        assertEquals(listOf(InferenceBackend.GPU, InferenceBackend.CPU), attempts)
        assertTrue(task.fellBack)
        task.close()
    }
    @Test fun gpuInferenceFailureReleasesGpuAndRetriesSameOperationOnCpu() {
        val created = mutableListOf<Engine>()
        val task = AcceleratedTask { Engine(it).also(created::add) }
        val result = task.run { if (it.backend == InferenceBackend.GPU) error("GPU runtime failed"); "ok" }
        assertEquals("ok", result)
        assertTrue(created[0].closed)
        assertEquals(InferenceBackend.CPU, task.backend)
        assertTrue(task.fellBack)
        task.close()
        assertTrue(created[1].closed)
    }
    @Test fun cpuComparisonDoesNotTryGpu() {
        val backends = mutableListOf<InferenceBackend>()
        val task = AcceleratedTask(preferGpu = false) { Engine(it).also { backends.add(it.backend) } }
        assertEquals(InferenceBackend.CPU, task.run { it.backend })
        assertEquals(listOf(InferenceBackend.CPU), backends)
        assertFalse(task.fellBack)
        task.close()
    }
    @Test fun cancellationDoesNotStartCpuFallback() {
        val created = mutableListOf<Engine>()
        val task = AcceleratedTask { Engine(it).also(created::add) }
        assertThrows(CancellationException::class.java) { task.run<Int> { throw CancellationException() } }
        assertEquals(1, created.size)
        assertEquals(InferenceBackend.GPU, created.single().backend)
        assertFalse(task.fellBack)
        task.close()
    }
}
