package cn.lemondrop.fhreborn.ui.components

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 歌曲封面位图内存缓存（按 songId）。
 *
 * 列表滚动时 LazyColumn 回收/重建 item，若无缓存每个 item 都要重新走
 * IO 解码（content://media 读取 + BitmapFactory），导致滚动掉帧；
 * 缓存命中后直接返回已解码的 ImageBitmap，滚动顺滑。
 * LruCache 线程安全，容量按张数计。
 */
object CoverImageCache {

    private const val CACHE_SIZE = 128

    private val cache = object : LruCache<Long, ImageBitmap>(CACHE_SIZE) {}

    fun get(songId: Long): ImageBitmap? = cache.get(songId)

    fun put(songId: Long, bitmap: ImageBitmap) {
        cache.put(songId, bitmap)
    }

    fun clear() = cache.evictAll()
}
