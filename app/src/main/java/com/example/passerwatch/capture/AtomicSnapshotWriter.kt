package com.example.passerwatch.capture

import java.io.File
import java.io.IOException
import java.io.OutputStream

class StorageFullException : IOException("可用存储不足，保存已暂停")
class AtomicSnapshotWriter(private val directory: File, private val availableBytes: () -> Long = { directory.usableSpace }) {
    fun write(name: String, encode: (OutputStream) -> Boolean): File {
        require(name.endsWith(".jpg") && name == File(name).name)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("无法创建图片目录")
        if (availableBytes() < 4L * 1024 * 1024) throw StorageFullException()
        val final = File(directory, name)
        val temporary = File(directory, "$name.tmp")
        if (final.exists()) throw IOException("图片文件名冲突")
        try {
            temporary.outputStream().use { stream ->
                if (!encode(stream)) throw IOException("JPEG 压缩失败")
            }
            if (temporary.length() == 0L) throw IOException("图片为空")
            if (!temporary.renameTo(final)) throw IOException("无法完成图片保存")
            return final
        } finally { temporary.delete() }
    }

    fun latest(): File? {
        var newest: File? = null
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "jpg" && file.length() > 0) {
                val current = newest
                if (current == null || file.lastModified() > current.lastModified() ||
                    (file.lastModified() == current.lastModified() && file.name > current.name)) newest = file
            }
        }
        return newest
    }
    fun cleanTemporaryFiles() {
        directory.listFiles()?.filter { it.isFile && it.extension == "tmp" }?.forEach { it.delete() }
    }
}
