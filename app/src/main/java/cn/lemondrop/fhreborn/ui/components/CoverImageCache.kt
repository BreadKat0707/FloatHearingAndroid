package cn.lemondrop.fhreborn.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 歌曲封面位图内存缓存（按 songId + 目标分辨率）。
 *
 * 列表滚动时 LazyColumn 回收/重建 item，若无缓存每个 item 都要重新走 IO
 * 解码，导致滚动掉帧。缓存命中后直接返回已解码的 ImageBitmap；容量按位图
 * 占用字节计算，避免把大尺寸封面全部塞进内存。
 */
object CoverImageCache {

    const val SMALL_COVER_TARGET = 256
    const val MEDIUM_COVER_TARGET = 384
    const val DEFAULT_COVER_TARGET = 600
    const val LARGE_COVER_TARGET = 900

    private const val MAX_MEMORY_BYTES = 64 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return value.width * value.height * 4
        }
    }

    fun get(songId: Long, targetSize: Int = DEFAULT_COVER_TARGET): ImageBitmap? =
        cache.get(key(songId, targetSize))

    fun put(songId: Long, targetSize: Int, bitmap: ImageBitmap) {
        val size = bitmap.width * bitmap.height * 4
        if (size > 0 && size <= MAX_MEMORY_BYTES) {
            cache.put(key(songId, targetSize), bitmap)
        }
    }

    fun clear() = cache.evictAll()

    private fun key(songId: Long, targetSize: Int): String = "$songId:$targetSize"
}

/**
 * 封面解码统一入口：先用 inJustDecodeBounds 读尺寸，再按目标边长降采样，
 * 避免 MediaStore/嵌入封面以原始分辨率整张解码造成卡顿和内存峰值。
 */
internal object CoverImageDecoder {
    const val DEFAULT_TARGET_SIZE = 600

    fun decodeScaled(bytes: ByteArray, targetSize: Int = DEFAULT_TARGET_SIZE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, targetSize)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }.getOrNull()
    }

    fun decodeScaled(context: Context, uri: Uri, targetSize: Int = DEFAULT_TARGET_SIZE): Bitmap? {
        val bounds = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { options ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } ?: return null
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, targetSize)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        if (width <= 0 || height <= 0 || targetSize <= 0) return 1
        var sampleSize = 1
        val longestSide = maxOf(width, height)
        while (longestSide / (sampleSize * 2) >= targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
