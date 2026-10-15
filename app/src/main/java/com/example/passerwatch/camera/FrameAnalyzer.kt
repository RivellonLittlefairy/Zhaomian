package com.example.passerwatch.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.passerwatch.detection.PersonDetector
import com.example.passerwatch.model.PersonResult
import com.example.passerwatch.model.DetectionSettings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CancellationException

class FrameAnalyzer(
    private val context: Context,
    private val session: () -> Long?,
    private val onFrame: (Long?, OwnedFrame, PersonResult?) -> Unit,
    private val onError: (Long?, String) -> Unit,
    private val preferGpu: Boolean = true,
    private val settings: DetectionSettings = DetectionSettings(),
    private val onDiagnostic: (String, String) -> Unit = { _, _ -> }
) : ImageAnalysis.Analyzer, AutoCloseable {
    private var detector: PersonDetector? = null
    private val sampler = FrameSampler(settings.targetFps)
    private val processingRate = ProcessingRate()
    private var previousSession: Long? = null
    private var frameCount = 0
    private val manual = AtomicBoolean(false)
    private val diagnostic = AtomicBoolean(false)
    fun requestDiagnosticFrame() { diagnostic.set(true) }
    @Volatile private var closed = false
    @Volatile private var intakePaused = false
    @Volatile private var digitalZoom = 1f
    fun digitalZoom(ratio: Float) { digitalZoom = ratio.coerceAtLeast(1f) }
    private val intakeEpoch = java.util.concurrent.atomic.AtomicLong()
    fun pauseIntake(paused: Boolean) { if (paused) intakeEpoch.incrementAndGet(); intakePaused = paused }
    fun requestManualFrame() { if (!closed) manual.set(true) }

    override fun analyze(image: ImageProxy) {
        var frame: OwnedFrame? = null
        var imageClosed = false
        val id = session()
        val epoch = intakeEpoch.get()
        try {
            if (closed || intakePaused) return
            val now = SystemClock.elapsedRealtime()
            if (id == null && !manual.get()) return
            if (id != previousSession) { sampler.reset(); processingRate.reset(); previousSession = id }
            if (!sampler.accept(now)) return
            if (id == null && !manual.getAndSet(false)) return
            if (id != null) manual.set(false)
            // Decode YUV explicitly: some physical-camera RGBA conversion paths yielded solid green.
            val rotationDegrees = image.imageInfo.rotationDegrees
            val source = YuvFrameDecoder.decode(image)
            val crop = image.cropRect
            val rotatedSource = try {
                Bitmap.createBitmap(source, crop.left, crop.top, crop.width(), crop.height(),
                    Matrix().apply { postRotate(rotationDegrees.toFloat()) }, true)
            } catch (e: Exception) { source.recycle(); throw e }
            if (source !== rotatedSource) source.recycle()
            val rotated = DigitalZoom.cropOwned(rotatedSource, digitalZoom)
            frame = OwnedFrame(rotated, now)
            // Release the camera buffer before potentially expensive inference or IO submission.
            image.close()
            imageClosed = true
            val result = if (id != null) {
                if (closed || intakePaused || intakeEpoch.get() != epoch || session() != id) return
                val model = detector ?: PersonDetector(context, preferGpu, settings).also { detector = it }
                val detected = model.detect(rotated) { !closed && !intakePaused && intakeEpoch.get() == epoch && session() == id }
                detected.copy(actualFps = processingRate.record(SystemClock.elapsedRealtime()))
            } else null
            if (com.example.passerwatch.BuildConfig.DEBUG && diagnostic.getAndSet(false)) {
                val file = java.io.File(context.cacheDir, "model-input.png")
                file.outputStream().use { rotated.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val summary = "${rotated.width}x${rotated.height} rotation=$rotationDegrees zoom=$digitalZoom " +
                    "${result?.reason} ${result?.diagnostics} ${result?.acceleration}"
                Log.i("Zhaomian", "model input diagnostic $summary")
                onDiagnostic(file.absolutePath, summary)
            }
            if (id != null && ++frameCount % 25 == 0) Log.i("Zhaomian",
                "inference=${result?.inferenceMs}ms fps=${result?.actualFps} image=${rotated.width}x${rotated.height} ${result?.acceleration} ${result?.stageTiming} ${result?.reason} ${result?.diagnostics}")
            if (!closed && !intakePaused && intakeEpoch.get() == epoch) onFrame(id, frame, result)
        } catch (_: CancellationException) {
            // Already-running native calls finish, but no subsequent model starts after a stop checkpoint.
        } catch (e: Exception) {
            Log.e("Zhaomian", "frame processing failed", e)
            if (!closed && !intakePaused && intakeEpoch.get() == epoch) onError(id, e.message ?: "图像或模型处理失败")
        } finally {
            frame?.close()
            if (!imageClosed) image.close()
        }
    }
    // Called after outstanding analysis on its own executor.
    override fun close() { closed = true; manual.set(false); detector?.close(); detector = null }
    fun stopIntake() { closed = true; manual.set(false) }
}
