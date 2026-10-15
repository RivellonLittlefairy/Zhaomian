package com.example.passerwatch.capture

import java.io.File
import java.io.OutputStream

interface GalleryEntry {
    fun open(): OutputStream
    fun publish(): String
    fun discard()
}
fun interface GallerySink {
    fun reserve(name: String): GalleryEntry
    fun findUri(name: String): String? = null
}

class GalleryPublisher(private val sink: GallerySink) {
    fun findUri(name: String): String? = sink.findUri(name)
    fun publish(file: File): String {
        require(file.isFile && file.length() > 0) { "没有可导出的图片" }
        val entry = sink.reserve(file.name)
        try {
            entry.open().use { output -> file.inputStream().use { it.copyTo(output) } }
            return entry.publish()
        } catch (e: Exception) {
            runCatching { entry.discard() }
            throw e
        }
    }
}
