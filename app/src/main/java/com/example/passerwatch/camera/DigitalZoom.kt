package com.example.passerwatch.camera

import android.graphics.Bitmap

/** Digital zoom for fixed physical streams. All outputs use the same centered source fraction. */
object DigitalZoom {
    fun cropOwned(source: Bitmap, ratio: Float): Bitmap {
        val zoom = ratio.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
        if (zoom <= 1.0001f) return source
        val width = (source.width / zoom).toInt().coerceIn(1, source.width)
        val height = (source.height / zoom).toInt().coerceIn(1, source.height)
        val cropped = Bitmap.createBitmap(source, (source.width - width) / 2, (source.height - height) / 2, width, height)
        if (cropped !== source) source.recycle()
        return cropped
    }
}
