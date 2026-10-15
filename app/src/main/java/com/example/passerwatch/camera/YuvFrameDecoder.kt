package com.example.passerwatch.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy

/** Converts the actual Y, U, V planes, respecting each plane's independent row/pixel strides. */
object YuvFrameDecoder {
    fun decode(image: ImageProxy): Bitmap {
        if (image.format != ImageFormat.YUV_420_888) return image.toBitmap()
        require(image.planes.size == 3) { "YUV 输入缺少色度平面" }
        val width = image.width
        val height = image.height
        val y = image.planes[0]; val u = image.planes[1]; val v = image.planes[2]
        val yb = y.buffer.duplicate(); val ub = u.buffer.duplicate(); val vb = v.buffer.duplicate()
        val yp = yb.position(); val up = ub.position(); val vp = vb.position()
        val colors = IntArray(width * height)
        for (row in 0 until height) {
            val yRow = yp + row * y.rowStride
            val uRow = up + (row / 2) * u.rowStride
            val vRow = vp + (row / 2) * v.rowStride
            for (column in 0 until width) {
                val luma = (yb.get(yRow + column * y.pixelStride).toInt() and 255) - 16
                val blue = (ub.get(uRow + (column / 2) * u.pixelStride).toInt() and 255) - 128
                val red = (vb.get(vRow + (column / 2) * v.pixelStride).toInt() and 255) - 128
                val scaled = 298 * luma.coerceAtLeast(0)
                val r = ((scaled + 409 * red + 128) shr 8).coerceIn(0, 255)
                val g = ((scaled - 100 * blue - 208 * red + 128) shr 8).coerceIn(0, 255)
                val b = ((scaled + 516 * blue + 128) shr 8).coerceIn(0, 255)
                colors[row * width + column] = (255 shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
    }
}
