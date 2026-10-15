package com.example.passerwatch.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.example.passerwatch.ZhaomianApplication
import com.example.passerwatch.camera.OwnedFrame
import com.example.passerwatch.capture.CaptureGate
import com.example.passerwatch.capture.CaptureRepository
import com.example.passerwatch.model.MonitorUiState
import com.example.passerwatch.model.PersonResult
import com.example.passerwatch.model.DetectionSettings
import com.example.passerwatch.monitor.SessionTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MonitorViewModel @JvmOverloads constructor(
    application: Application,
    private val captures: CaptureRepository = (application as ZhaomianApplication).captures
) : AndroidViewModel(application) {
    val storage = captures.state
    val records = captures.records
    private val preferences = application.getSharedPreferences("sentry-settings", android.content.Context.MODE_PRIVATE)
    private fun loadSettings() = DetectionSettings(
        targetFps = preferences.getInt("fps", 20).coerceIn(1, 30),
        scoreThreshold = preferences.getFloat("score", 0.6f).takeIf { it.isFinite() }?.coerceIn(0.3f, 0.95f) ?: 0.6f,
        confirmationFrames = preferences.getInt("frames", 2).coerceIn(1, 5),
        vacantSeconds = preferences.getFloat("vacant", 2f).takeIf { it.isFinite() }?.coerceIn(1f, 5f) ?: 2f)
    private val mutable = MutableStateFlow(MonitorUiState(settings = loadSettings(), darkTheme = preferences.getBoolean("dark-theme", true)))
    val state = mutable.asStateFlow()
    private val sessions = SessionTracker()
    private var gate = CaptureGate(mutable.value.settings.confirmationFrames, mutable.value.settings.vacantMs, mutable.value.settings.maxResultGapMs)
    private val lock = Any()
    @Volatile private var screenActive = false
    private var sessionFrames = 0
    private var lastUiUpdate = -250L
    private var photoRequest: ((Long?) -> Unit)? = null
    fun photoRequester(request: ((Long?) -> Unit)?) = synchronized(lock) { photoRequest = request }
    fun photoCaptured(id: Long?, bitmap: Bitmap) { captures.submit(bitmap, id) }
    fun photoError(message: String) { mutable.update { it.copy(error = message) } }
    fun sessionId(): Long? = mutable.value.takeIf { it.running }?.sessionId

    fun screenStarted() { screenActive = true; captures.reloadLatest() }
    fun screenStopped() { screenActive = false; stop() }
    fun start() = synchronized(lock) {
        if (!screenActive || !mutable.value.cameraReady || !storage.value.initialized) return@synchronized
        if (mutable.value.running) return@synchronized
        gate.reset()
        sessionFrames = 0; lastUiUpdate = -250L
        val id = sessions.start()
        mutable.update { it.copy(running = true, sessionId = id, monitorText = "等待可见人脸或完整身体 · 正在准备模型",
            error = null, processedFrames = 0, actualFps = 0f, stageTiming = "", acceleration = "等待模型加载") }
    }
    fun stop() = synchronized(lock) {
        sessions.stop(); gate.reset()
        mutable.update { it.copy(running = false, monitorText = "未开始检测") }
    }
    fun cameraReady(ready: Boolean) {
        mutable.update { it.copy(cameraReady = ready) }
    }
    fun cameraSwitchStarted() = synchronized(lock) {
        gate.interrupt()
        mutable.update { it.copy(cameraReady = false, monitorText = if (it.running) "正在自动切换镜头" else it.monitorText) }
    }
    fun cameraError(message: String) { stop(); mutable.update { it.copy(cameraReady = false, error = message) } }
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun applySettings(settings: DetectionSettings) = synchronized(lock) {
        if (mutable.value.running) return@synchronized
        gate = CaptureGate(settings.confirmationFrames, settings.vacantMs, settings.maxResultGapMs)
        preferences.edit().putInt("fps", settings.targetFps).putFloat("score", settings.scoreThreshold)
            .putInt("frames", settings.confirmationFrames).putFloat("vacant", settings.vacantSeconds).apply()
        mutable.update { it.copy(settings = settings, acceleration = "等待模型加载", actualFps = 0f,
            stageTiming = "", inferenceMs = 0, processedFrames = 0, error = null) }
    }
    fun toggleTheme() = synchronized(lock) {
        val dark = !mutable.value.darkTheme
        preferences.edit().putBoolean("dark-theme", dark).apply()
        mutable.update { it.copy(darkTheme = dark) }
    }
    fun reloadGallery() = captures.reloadLatest()
    suspend fun galleryUri(record: com.example.passerwatch.model.SnapshotRecord) = captures.resolveGalleryUri(record)
    fun resumeSaving() = captures.resumeSaving()
    fun lensesAvailable(lenses: List<com.example.passerwatch.camera.CameraLensOption>, selected: String) {
        // Discovery must not change the selection key: doing so would dispose this controller mid-bind.
        mutable.update { it.copy(lenses = lenses, activeLensKey = selected, lensEvidence = "等待输出镜头核验") }
    }
    fun zoomChanged(ratio: Float, minimum: Float, maximum: Float) {
        mutable.update { it.copy(zoomRatio = ratio, minZoom = minimum, maxZoom = maximum) }
    }
    fun lensEvidenceChanged(evidence: String) { mutable.update { it.copy(lensEvidence = evidence) } }
    fun digitalZoomChanged(ratio: Float, aspect: Float) {
        mutable.update { it.copy(digitalCropRatio = ratio, previewAspect = aspect) }
    }

    fun onFrame(id: Long?, frame: OwnedFrame, result: PersonResult?) = synchronized(lock) {
        if (!screenActive) return@synchronized
        if (id == null) {
            if (!mutable.value.running) photoRequest?.invoke(null)
            return@synchronized
        }
        if (!sessions.accepts(id) || result == null) return@synchronized
        val trigger = gate.accept(result.hasPerson, frame.elapsedMs, result.eligibleForCapture)
        sessionFrames++
        val text = if (gate.waitingForDeparture) "已触发，等待人离开"
            else if (result.hasPerson) "${result.reason.ifBlank { "等待符合条件的人" }}"
            else "等待可见人脸或完整身体"
        // Gate processes every accepted frame; only visible metrics are throttled to avoid 20 UI recompositions/sec.
        if (trigger || text != mutable.value.monitorText || frame.elapsedMs - lastUiUpdate >= 250) {
            lastUiUpdate = frame.elapsedMs
            mutable.update { it.copy(monitorText = text, inferenceMs = result.inferenceMs, processedFrames = sessionFrames,
                acceleration = result.acceleration, actualFps = result.actualFps, stageTiming = result.stageTiming) }
        }
        if (trigger) {
            val request = photoRequest
            if (request == null) photoError("拍照未就绪，请重试相机") else request(id)
        }
    }
    fun analysisError(id: Long?, message: String) = synchronized(lock) {
        if (!screenActive || (id != null && !sessions.accepts(id))) return@synchronized
        stop()
        mutable.update { it.copy(error = "检测失败：$message。可重新开始检测。") }
    }
    override fun onCleared() { screenStopped(); super.onCleared() }
}

