package com.example.passerwatch.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.tasks.core.Delegate
import com.example.passerwatch.model.PersonResult
import com.example.passerwatch.model.DetectionSettings
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.concurrent.CancellationException

// All models and owned MPImages stay on the single Analyzer thread.
class PersonDetector(
    context: Context,
    preferGpu: Boolean = true,
    private val settings: DetectionSettings = DetectionSettings()
) : AutoCloseable {
    private val app = context.applicationContext
    private fun base(path: String, backend: InferenceBackend) = BaseOptions.builder()
        .setModelAssetPath(path).setDelegate(if (backend == InferenceBackend.GPU) Delegate.GPU else Delegate.CPU).build()
    private val faceTask = AcceleratedTask<FaceDetector>(preferGpu,
        onFallback = { Log.w("Zhaomian", "Face GPU unavailable; falling back to CPU", it) }) { backend ->
        FaceDetector.createFromOptions(app, FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(base("blaze_face_short_range.tflite", backend))
            .setRunningMode(RunningMode.IMAGE).setMinDetectionConfidence(settings.scoreThreshold).build())
    }
    private val personTask = AcceleratedTask<ObjectDetector>(preferGpu,
        onFallback = { Log.w("Zhaomian", "Person GPU unavailable; falling back to CPU", it) }) { backend ->
        // The advanced GPU path consumes float input buffers; the int8 model has UINT8 input.
        // Keep the smaller quantized model for CPU, including GPU failure fallback.
        val model = if (backend == InferenceBackend.GPU) "efficientdet_lite0_float16.tflite"
            else "efficientdet_lite0.tflite"
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(base(model, backend))
            .setRunningMode(RunningMode.IMAGE).setScoreThreshold(settings.scoreThreshold)
        // MediaPipe 0.10.21 GPU postprocessing aborts natively for a partial allowlist.
        // GPU returns all classes; detect() below filters person in Kotlin.
        if (backend == InferenceBackend.CPU) options.setCategoryAllowlist(listOf("person"))
        ObjectDetector.createFromOptions(app, options.build()).also {
                Log.i("Zhaomian", "Person model created: backend=$backend asset=$model")
            }
    }
    private val poseTask = AcceleratedTask<PoseLandmarker>(preferGpu,
        onFallback = { Log.w("Zhaomian", "Pose GPU unavailable; falling back to CPU", it) }) { backend ->
        PoseLandmarker.createFromOptions(app, PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(base("pose_landmarker_lite.task", backend))
            .setRunningMode(RunningMode.IMAGE).setNumPoses(2)
            .setMinPoseDetectionConfidence(settings.scoreThreshold)
            .setMinPosePresenceConfidence(settings.scoreThreshold).build())
    }
    private fun mode(name: String, task: AcceleratedTask<*>) = "$name ${task.backend?.name ?: "未加载"}${if (task.fellBack) "（回退）" else ""}"
    private fun acceleration() = listOf(mode("人脸", faceTask), mode("人员", personTask), mode("身体", poseTask)).joinToString(" / ")

    fun detect(bitmap: Bitmap, stillActive: () -> Boolean = { true }): PersonResult {
        val started = SystemClock.elapsedRealtime()
        fun checkpoint() { if (!stillActive()) throw CancellationException("Monitoring stopped") }
        checkpoint()
        // MPImage.close() recycles its Bitmap; never give it the original trigger frame.
        val inferenceBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val image = try { BitmapImageBuilder(inferenceBitmap).build() }
            catch (e: Exception) { inferenceBitmap.recycle(); throw e }
        try {
            val faceStart = SystemClock.elapsedRealtime()
            val faceResult = faceTask.run(stillActive) { checkpoint(); it.detect(image) }
            val faceMs = SystemClock.elapsedRealtime() - faceStart
            val visibleFace = faceResult.detections().any { detection ->
                detection.categories().any { it.score() >= settings.scoreThreshold }
            }
            if (visibleFace) return PersonResult(true, SystemClock.elapsedRealtime() - started, true, "人脸可见",
                acceleration = acceleration(), stageTiming = "脸 ${faceMs}ms",
                diagnostics = "faces=${faceResult.detections().size}")
            checkpoint()
            val personStart = SystemClock.elapsedRealtime()
            val personResult = personTask.run(stillActive) { checkpoint(); it.detect(image) }
            val personMs = SystemClock.elapsedRealtime() - personStart
            val personPresent = personResult.detections().any { detection ->
                detection.categories().any { it.categoryName() == "person" && it.score() >= settings.scoreThreshold }
            }
            checkpoint()
            val poseStart = SystemClock.elapsedRealtime()
            val poseResult = poseTask.run(stillActive) { checkpoint(); it.detect(image) }
            val poseMs = SystemClock.elapsedRealtime() - poseStart
            val bodies = poseResult.landmarks().map { landmarks ->
                landmarks.map { BodyPoint(it.x(), it.y(), it.visibility().orElse(0f), it.presence().orElse(0f)) }
            }
            val wholeBody = bodies.any { FullBodyPolicy.accepts(it, bitmap.width.toFloat() / bitmap.height) }
            val person = personPresent || bodies.isNotEmpty()
            return PersonResult(person, SystemClock.elapsedRealtime() - started, wholeBody,
                if (wholeBody) "完整身体可见（无需清晰人脸）"
                else if (person) "有人，但未确认可见脸或完整身体" else "未检测到人",
                acceleration = acceleration(), stageTiming = "脸 ${faceMs}ms · 人员 ${personMs}ms · 身体 ${poseMs}ms",
                diagnostics = "faces=${faceResult.detections().size} objects=${personResult.detections().size} poses=${bodies.size} " +
                    personResult.detections().take(3).joinToString { it.categories().joinToString { c -> "${c.categoryName()}:${c.score()}" } })
        } finally { image.close() }
    }
    override fun close() {
        try { faceTask.close() }
        finally { try { personTask.close() } finally { poseTask.close() } }
    }
}
