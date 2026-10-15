package com.example.passerwatch.camera

import android.graphics.Bitmap

class OwnedFrame(val bitmap: Bitmap, val elapsedMs: Long) : AutoCloseable {
    override fun close() { bitmap.recycle() }
}
