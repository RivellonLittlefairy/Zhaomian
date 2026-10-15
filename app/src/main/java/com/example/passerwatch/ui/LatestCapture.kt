package com.example.passerwatch.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

@Composable
fun LocalCaptureImage(path: String, full: Boolean = false, showMetadata: Boolean = true) {
    key(path, full) { CaptureImageForPath(path, full, showMetadata) }
}

private sealed interface ImageLoad {
    data object Loading : ImageLoad
    data object Failed : ImageLoad
    data class Loaded(val bitmap: ImageBitmap, val width: Int, val height: Int) : ImageLoad
}

@Composable
private fun CaptureImageForPath(path: String, full: Boolean, showMetadata: Boolean) {
    val image by produceState<ImageLoad>(ImageLoad.Loading, path, full) {
        value = try { withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext ImageLoad.Failed
            val maxSize = if (full) 2048 else 480
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSize) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                ?.let { ImageLoad.Loaded(it, bounds.outWidth, bounds.outHeight) } ?: ImageLoad.Failed
        } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { ImageLoad.Failed }
    }
    when (val loaded = image) {
        ImageLoad.Loading -> Text("正在读取图片…")
        ImageLoad.Failed -> Text("无法打开图片，文件可能已损坏")
        is ImageLoad.Loaded -> {
            Image(loaded.bitmap, "抓拍现场图片", modifier = Modifier.fillMaxWidth()
            .heightIn(max = if (full) 560.dp else 200.dp), contentScale = ContentScale.Fit)
            if (showMetadata) Text("照片尺寸 ${loaded.width} × ${loaded.height}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
