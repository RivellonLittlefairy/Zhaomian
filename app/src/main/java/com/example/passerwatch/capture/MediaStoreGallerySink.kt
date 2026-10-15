package com.example.passerwatch.capture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MediaStoreGallerySink(context: Context) : GallerySink {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    override fun findUri(name: String): String? {
        val selection: String
        val selectionArgs: Array<String>
        if (Build.VERSION.SDK_INT >= 29) {
            // Keep old captures discoverable after the app's rename.
            selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} IN (?, ?) AND ${MediaStore.Images.Media.IS_PENDING} = 0"
            selectionArgs = arrayOf(name, "${Environment.DIRECTORY_PICTURES}/Zhaomian/",
                "${Environment.DIRECTORY_PICTURES}/PasserWatch/")
        } else {
            @Suppress("DEPRECATION") val dataColumn = MediaStore.Images.Media.DATA
            @Suppress("DEPRECATION") val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zhaomian/$name")
            @Suppress("DEPRECATION") val legacy = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PasserWatch/$name")
            selection = "$dataColumn IN (?, ?)"
            selectionArgs = arrayOf(file.absolutePath, legacy.absolutePath)
        }
        resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID), selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                cursor.getLong(0)).toString()
        }
        return null
    }
    override fun reserve(name: String): GalleryEntry {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Zhaomian")
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                throw SecurityException("请授予相册保存权限")
            @Suppress("DEPRECATION") val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zhaomian")
            if (!folder.exists() && !folder.mkdirs()) throw IOException("无法创建相册目录")
            @Suppress("DEPRECATION") val dataColumn = MediaStore.Images.Media.DATA
            values.put(dataColumn, File(folder, name).absolutePath)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("无法创建相册图片")
        return object : GalleryEntry {
            override fun open() = resolver.openOutputStream(uri, "w") ?: throw IOException("无法写入相册图片")
            override fun publish(): String {
                if (Build.VERSION.SDK_INT >= 29) {
                    val updated = resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                    if (updated != 1) throw IOException("相册图片发布失败")
                }
                return uri.toString()
            }
            override fun discard() { resolver.delete(uri, null, null) }
        }
    }
}
