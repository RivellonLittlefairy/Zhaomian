package com.example.passerwatch.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.Executors

@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
class CameraController(
    context: Context,
    private val analyzer: FrameAnalyzer,
    private val ready: (Boolean) -> Unit,
    private val error: (String) -> Unit,
    private val captured: (Long?, Bitmap) -> Unit,
    private val captureError: (String) -> Unit,
    private val activeSession: () -> Long?,
    private val onLenses: (List<CameraLensOption>, String) -> Unit = { _, _ -> },
    private val onZoom: (Float, Float, Float) -> Unit = { _, _, _ -> },
    private val onLensEvidence: (String) -> Unit = {},
    private val onSwitch: () -> Unit = {},
    private val onDigitalZoom: (Float, Float) -> Unit = { _, _ -> }
) : AutoCloseable {
    private val application = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(application)
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "person-analyzer") }
    private var provider: ProcessCameraProvider? = null
    private var owner: LifecycleOwner? = null
    private var view: PreviewView? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var photo: ImageCapture? = null
    private var takingPhoto = false
    private var disposed = false
    private var cameraState: LiveData<CameraState>? = null
    private var stateObserver: Observer<CameraState>? = null
    private var boundCamera: Camera? = null
    private var zoomState: LiveData<ZoomState>? = null
    private var zoomObserver: Observer<ZoomState>? = null
    private var lastEvidence = ""
    private var policy: AutoLensPolicy? = null
    private var options = emptyList<CameraLensOption>()
    private var chosen: CameraLensOption? = null
    private val unavailable = mutableSetOf<String>()
    private var desiredZoom = 1f
    private var generation = 0
    @Volatile private var digitalCrop = 1f
    private var outputAspect = .75f
    private var switchScheduled = false
    private var lastSwitch = 0L
    private val applySelection: Runnable = object : Runnable { override fun run() {
        switchScheduled = false
        if (!disposed) {
            val target = policy?.choose(desiredZoom, unavailable)
            if (target != null && target.key != chosen?.key) {
                if (takingPhoto) { switchScheduled = true; handler.postDelayed(this, 60) } else bindLens(target)
            } else applyNativeZoom()
        }
    } }

    fun bind(owner: LifecycleOwner, view: PreviewView) {
        this.owner = owner; this.view = view
        val future = ProcessCameraProvider.getInstance(application)
        future.addListener({
            if (!disposed) try {
                provider = future.get()
                options = CameraLensCatalog.discover(provider!!)
                policy = AutoLensPolicy(options)
                desiredZoom = 1f.coerceIn(policy!!.minimum, policy!!.maximum)
                bindLens(policy!!.choose(desiredZoom))
            } catch (e: Exception) { ready(false); error("相机启动失败：${e.message}") }
        }, mainExecutor)
    }

    private fun detachStreams() {
        stateObserver?.let { cameraState?.removeObserver(it) }
        zoomObserver?.let { zoomState?.removeObserver(it) }
        stateObserver = null; cameraState = null; zoomObserver = null; zoomState = null; boundCamera = null
        analysis?.clearAnalyzer()
        val uses = listOfNotNull(preview, analysis, photo).toTypedArray()
        if (uses.isNotEmpty()) provider?.unbind(*uses)
        preview = null; analysis = null; photo = null
    }

    private fun bindLens(target: CameraLensOption) {
        if (disposed) return
        analyzer.pauseIntake(true); ready(false); onSwitch()
        detachStreams()
        val token = ++generation
        lastSwitch = android.os.SystemClock.elapsedRealtime()
        chosen = target; lastEvidence = ""
        onLenses(options, target.key)
        try {
            val rotation = view?.display?.rotation ?: android.view.Surface.ROTATION_0
            val pb = Preview.Builder().setTargetRotation(rotation)
            val ab = ImageAnalysis.Builder().setTargetRotation(rotation)
                .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            val cb = ImageCapture.Builder().setTargetRotation(rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setJpegQuality(95)
                .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(1920, 1440), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
            target.physicalId?.let { physical ->
                if (Build.VERSION.SDK_INT >= 28) {
                    Camera2Interop.Extender(pb).setPhysicalCameraId(physical)
                    // Some HALs expose physical PRIV/JPEG but return empty physical YUV buffers.
                    // Keep analysis on the logical stream; preview and photo remain on the optical lens.
                    Camera2Interop.Extender(cb).setPhysicalCameraId(physical)
                } else throw IllegalStateException("当前系统不支持物理输出")
            }
            Camera2Interop.Extender(pb).setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                private var frames = 0
                @Suppress("DEPRECATION")
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    if (Build.VERSION.SDK_INT < 28 || (++frames != 1 && frames % 30 != 0)) return
                    val physical = result.physicalCameraResults
                    val actual = if (Build.VERSION.SDK_INT >= 29) result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID) else null
                    val focal = (target.physicalId?.let { physical[it] } ?: if (target.physicalId == null) result else null)
                        ?.get(CaptureResult.LENS_FOCAL_LENGTH)
                    val evidence = if (target.physicalId != null) "自动焦段 · ${target.label} · ${if (physical.containsKey(target.physicalId)) "已返回物理元数据" else "等待物理元数据"}"
                        else "系统自动选镜 · 当前物理 ${actual ?: "未报告"}"
                    mainExecutor.execute {
                        if (!disposed && token == generation && evidence != lastEvidence) {
                            lastEvidence = evidence; onLensEvidence(evidence)
                            Log.i("Zhaomian", "auto lens=${target.key} base=${target.baseZoom} requested=$desiredZoom logicalActive=$actual physicalResults=${physical.keys} focal=$focal")
                        }
                    }
                }
            })
            val p = pb.build().also { it.setSurfaceProvider(view!!.surfaceProvider) }
            val a = ab.build().also { it.setAnalyzer(worker, analyzer) }
            val c = cb.build()
            preview = p; analysis = a; photo = c
            val camera = provider!!.bindToLifecycle(owner!!, CameraLensCatalog.selector(target), p, a, c)
            boundCamera = camera
            p.resolutionInfo?.let { info ->
                outputAspect = if (info.rotationDegrees % 180 == 0) info.resolution.width.toFloat() / info.resolution.height
                    else info.resolution.height.toFloat() / info.resolution.width
            }
            val stateListener = Observer<CameraState> { state ->
                if (!disposed && token == generation) {
                    if (state.error != null) {
                        if (target.physicalId != null) {
                            unavailable.add(target.key)
                            handler.post { if (!disposed && token == generation) bindLens(policy!!.fallback) }
                        } else { ready(false); error("相机不可用（错误 ${state.error!!.code}）") }
                    } else if (state.type == CameraState.Type.OPEN) applyNativeZoom()
                    else { analyzer.pauseIntake(true); ready(false) }
                }
            }
            cameraState = camera.cameraInfo.cameraState; stateObserver = stateListener
            cameraState?.observe(owner!!, stateListener)
            val zoomListener = Observer<ZoomState> { zoom ->
                if (!disposed && token == generation) {
                    if (target.physicalId != null) return@Observer
                    val scale = target.baseZoom ?: 1f
                    val actual = zoom.zoomRatio * scale
                    val globalMin = if (target.physicalId == null && policy!!.steps.isEmpty()) zoom.minZoomRatio else policy!!.minimum
                    val globalMax = if (target.physicalId == null && policy!!.steps.isEmpty()) zoom.maxZoomRatio else policy!!.maximum
                    onZoom(actual, globalMin, globalMax)
                    val requested = nativeZoom(target, desiredZoom, zoom)
                    if (kotlin.math.abs(zoom.zoomRatio - requested) < .015f && cameraState?.value?.type == CameraState.Type.OPEN) {
                        analyzer.pauseIntake(false); ready(true)
                    }
                }
            }
            zoomState = camera.cameraInfo.zoomState; zoomObserver = zoomListener
            zoomState?.observe(owner!!, zoomListener)
            applyNativeZoom()
            Log.i("Zhaomian", "auto bind lens=${target.key} opticalScale=${target.baseZoom} globalZoom=$desiredZoom analysis=${a.resolutionInfo?.resolution} photo=${c.resolutionInfo?.resolution}")
        } catch (e: Exception) {
            if (target.physicalId != null) {
                unavailable.add(target.key)
                Log.w("Zhaomian", "physical auto lens unavailable; using logical camera", e)
                bindLens(policy!!.fallback)
            } else { ready(false); error("相机启动失败：${e.message}") }
        }
    }

    private fun nativeZoom(lens: CameraLensOption, global: Float, limits: ZoomState): Float {
        val scale = lens.baseZoom ?: 1f
        val lower = if (lens.physicalId != null) maxOf(1f, limits.minZoomRatio) else limits.minZoomRatio
        val upper = minOf(limits.maxZoomRatio, lens.maximumNativeZoom).coerceAtLeast(lower)
        return (global / scale).coerceIn(lower, upper)
    }
    private fun applyNativeZoom() {
        val camera = boundCamera ?: return
        val target = chosen ?: return
        val limits = camera.cameraInfo.zoomState.value ?: return
        if (target.physicalId != null) {
            val scale = target.baseZoom ?: 1f
            digitalCrop = (desiredZoom / scale).coerceIn(1f, target.maximumNativeZoom)
            analyzer.digitalZoom(digitalCrop)
            onDigitalZoom(digitalCrop, outputAspect)
            onZoom(scale * digitalCrop, policy!!.minimum, policy!!.maximum)
            if (cameraState?.value?.type == CameraState.Type.OPEN) { analyzer.pauseIntake(false); ready(true) }
            return
        }
        digitalCrop = 1f; analyzer.digitalZoom(1f); onDigitalZoom(1f, outputAspect)
        val token = generation
        val operation = camera.cameraControl.setZoomRatio(nativeZoom(target, desiredZoom, limits))
        operation.addListener({
            if (!disposed && token == generation) try {
                operation.get()
                analyzer.pauseIntake(false); ready(true)
            } catch (e: Exception) {
                if (e.cause !is CameraControl.OperationCanceledException) {
                    if (target.physicalId != null) {
                        unavailable.add(target.key)
                        handler.post { if (!disposed && token == generation) bindLens(policy!!.fallback) }
                    } else error("缩放失败：${e.cause?.message ?: e.message}")
                }
            }
        }, mainExecutor)
    }
    fun requestManualFrame() = analyzer.requestManualFrame()
    fun requestDiagnosticFrame() = analyzer.requestDiagnosticFrame()
    fun setZoom(ratio: Float) = mainExecutor.execute {
        if (disposed || !ratio.isFinite()) return@execute
        val ladder = policy ?: return@execute
        val live = boundCamera?.cameraInfo?.zoomState?.value
        val min = if (ladder.steps.isEmpty()) live?.minZoomRatio ?: ladder.minimum else ladder.minimum
        val max = if (ladder.steps.isEmpty()) live?.maxZoomRatio ?: ladder.maximum else ladder.maximum
        desiredZoom = ratio.coerceIn(min, max)
        applyNativeZoom()
        // Throttle switches, rather than debounce: dragging must not indefinitely postpone them.
        if (!switchScheduled) {
            switchScheduled = true
            handler.postDelayed(applySelection, maxOf(0L, 180L - (android.os.SystemClock.elapsedRealtime() - lastSwitch)))
        }
    }
    // Requests may arrive on the analyzer thread; CameraX intake and lifecycle stay on main.
    fun takePhoto(sessionId: Long?) = mainExecutor.execute {
        if (disposed || (sessionId != null && activeSession() != sessionId)) return@execute
        val cameraPhoto = photo
        if (cameraPhoto == null || takingPhoto) {
            captureError("拍照未就绪或上一张照片尚未完成")
            return@execute
        }
        takingPhoto = true
        val photoCrop = digitalCrop
        try {
            cameraPhoto.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    if (disposed) { image.close(); takingPhoto = false; return }
                    try {
                        worker.execute {
                            var owned: Bitmap? = null
                            try {
                                val source = image.toBitmap()
                                val crop = image.cropRect
                                val rotatedSource = try {
                                    Bitmap.createBitmap(source, crop.left, crop.top, crop.width(), crop.height(),
                                        Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, true)
                                } catch (e: Exception) { source.recycle(); throw e }
                                if (source !== rotatedSource) source.recycle()
                                val rotated = DigitalZoom.cropOwned(rotatedSource, photoCrop)
                                owned = rotated
                                Log.i("Zhaomian", "photo captured=${rotated.width}x${rotated.height} session=$sessionId")
                                captured(sessionId, rotated)
                                owned = null // ownership transferred to the save queue
                            } catch (e: Exception) { captureError("拍照处理失败：${e.message}") }
                            finally {
                                owned?.recycle(); image.close()
                                mainExecutor.execute { takingPhoto = false }
                            }
                        }
                    } catch (e: Exception) {
                        image.close(); takingPhoto = false
                        captureError("拍照处理未完成：${e.message}")
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    takingPhoto = false
                    if (!disposed) captureError("拍照失败：${exception.message}")
                }
            })
        } catch (e: Exception) { takingPhoto = false; captureError("拍照失败：${e.message}") }
    }
    override fun close() {
        if (disposed) return
        disposed = true
        handler.removeCallbacks(applySelection)
        stateObserver?.let { cameraState?.removeObserver(it) }
        zoomObserver?.let { zoomState?.removeObserver(it) }
        zoomObserver = null; zoomState = null; boundCamera = null
        stateObserver = null; cameraState = null
        ready(false)
        analyzer.stopIntake()
        analysis?.clearAnalyzer()
        val uses = listOfNotNull(preview, analysis, photo).toTypedArray()
        if (uses.isNotEmpty()) provider?.unbind(*uses)
        worker.execute { analyzer.close() }
        worker.shutdown()
    }
}

