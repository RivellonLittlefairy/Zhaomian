package com.example.passerwatch.model

data class MonitorUiState(
    val running: Boolean = false,
    val sessionId: Long? = null,
    val cameraReady: Boolean = false,
    val monitorText: String = "未开始检测",
    val error: String? = null,
    val inferenceMs: Long = 0,
    val processedFrames: Int = 0,
    val acceleration: String = "等待模型加载",
    val actualFps: Float = 0f,
    val stageTiming: String = "",
    val settings: DetectionSettings = DetectionSettings(),
    val lenses: List<com.example.passerwatch.camera.CameraLensOption> = emptyList(),
    val activeLensKey: String? = null,
    val lensEvidence: String = "等待输出镜头核验",
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val digitalCropRatio: Float = 1f,
    val previewAspect: Float = 0.75f
)

data class SnapshotRecord(val path: String, val savedAt: Long, val bytes: Long, val galleryUri: String? = null)

data class StorageState(
    val initialized: Boolean = false,
    val latest: SnapshotRecord? = null,
    val pending: Int = 0,
    val paused: Boolean = false,
    val message: String = "正在读取本地图片…",
    val savedCounts: Map<Long, Int> = emptyMap()
)

data class PersonResult(
    val hasPerson: Boolean,
    val inferenceMs: Long,
    val eligibleForCapture: Boolean = hasPerson,
    val reason: String = "",
    val acceleration: String = "",
    val actualFps: Float = 0f,
    val stageTiming: String = "",
    val diagnostics: String = ""
)


