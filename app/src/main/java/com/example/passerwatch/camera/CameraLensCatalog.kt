package com.example.passerwatch.camera

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.Locale
import kotlin.math.sqrt

data class CameraLensOption(val key: String, val logicalId: String, val physicalId: String?, val label: String,
    val baseZoom: Float? = null, val maximumNativeZoom: Float = 1f)

@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
object CameraLensCatalog {
    fun discover(provider: ProcessCameraProvider): List<CameraLensOption> = buildList {
        provider.availableCameraInfos.filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }.forEach { info ->
            val id = Camera2CameraInfo.from(info).cameraId
            val reference = equivalent(info)
            add(CameraLensOption(id, id, null, "自动后置 ($id)", 1f, maximumZoom(info)))
            info.physicalCameraInfos.sortedBy { Camera2CameraInfo.from(it).cameraId }.forEach { lens ->
                val physicalId = Camera2CameraInfo.from(lens).cameraId
                val focal = equivalent(lens)
                val scale = if (reference != null && focal != null && reference > 0) (focal / reference).toFloat() else null
                val kind = when { scale == null -> "物理镜头"; scale < .85f -> "超广角"; scale > 1.3f -> "长焦"; else -> "主摄" }
                val detail = focal?.let { String.format(Locale.US, "约 %.0fmm 等效", it) }.orEmpty()
                add(CameraLensOption("$id/$physicalId", id, physicalId, "$kind · $detail ($physicalId)", scale, maximumZoom(lens)))
            }
        }
    }

    fun selector(option: CameraLensOption): CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == option.logicalId } }
        .build()

    private fun equivalent(info: CameraInfo): Double? {
        val metadata = Camera2CameraInfo.from(info)
        val focal = metadata.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: return null
        val sensor = metadata.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
        if (focal <= 0 || sensor.width <= 0 || sensor.height <= 0) return null
        return focal * 43.2666 / sqrt((sensor.width * sensor.width + sensor.height * sensor.height).toDouble())
    }
    private fun maximumZoom(info: CameraInfo): Float {
        val metadata = Camera2CameraInfo.from(info)
        val range = if (Build.VERSION.SDK_INT >= 30) metadata.getCameraCharacteristic(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) else null
        return (range?.upper ?: metadata.getCameraCharacteristic(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f).coerceAtLeast(1f)
    }
}

/** A pure, device-derived focal ladder. Thresholds are optical ratios, never fixed 3x or camera IDs. */
class AutoLensPolicy(options: List<CameraLensOption>) {
    val fallback = options.firstOrNull { it.physicalId == null } ?: error("没有后置相机")
    private val physical = options.filter { it.logicalId == fallback.logicalId && it.physicalId != null && it.baseZoom?.isFinite() == true && it.baseZoom > 0 }
        .sortedBy { it.baseZoom }
    val minimum = minOf(1f, physical.firstOrNull()?.baseZoom ?: 1f)
    val maximum = if (physical.isEmpty()) fallback.maximumNativeZoom else physical.maxOf { it.baseZoom!! * it.maximumNativeZoom }
    val steps = physical.mapNotNull { it.baseZoom }.distinct()
    fun choose(ratio: Float, unavailable: Set<String> = emptySet()): CameraLensOption {
        val eligible = physical.filter { it.key !in unavailable }
        return eligible.lastOrNull { it.baseZoom!! <= ratio + .0001f } ?: fallback
    }
}
