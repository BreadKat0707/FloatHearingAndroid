package cn.lemondrop.fhreborn.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.Song
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * 歌单封面：三态。
 * - coverSource=0（自动）：前 3 首歌曲封面拼图（随歌单内容变化）
 * - coverSource=1（选歌曲）：指定歌曲（coverPath 存 songId）的封面
 * - coverSource=2（自选图片）：coverPath 指向应用目录图片
 *
 * @param cornerRadius 封面自身圆角；在卡片视图等由外层容器裁切的场景传 0.dp
 */
@Composable
fun PlaylistCover(
    songIds: List<Long>,
    coverPath: String?,
    coverSource: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    songs: List<Song> = emptyList()
) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<ImageBitmap>?>(null) }

    LaunchedEffect(songIds, coverPath, coverSource, songs) {
        bitmaps = withContext(Dispatchers.IO) {
            when {
                coverSource == PlaylistCoverSource.SELF_IMAGE && coverPath != null ->
                    loadFileBitmap(context, coverPath)?.let { listOf(it) }

                coverSource == PlaylistCoverSource.SONG_COVER && coverPath != null -> {
                    val songId = coverPath.toLongOrNull()
                    val song = songs.firstOrNull { it.id == songId }
                    loadSongCover(context, songId, song)?.let { listOf(it) }
                }

                else -> songIds.take(3).mapIndexedNotNull { index, songId ->
                    loadSongCover(context, songId, songs.getOrNull(index))
                }
            }
        }
    }

    val size = modifier.let { /* 由外部控制尺寸 */ }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MiuixTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (bitmaps?.size) {
            1 -> Image(
                bitmap = bitmaps!![0],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            2 -> Row(modifier = Modifier.fillMaxSize()) {
                bitmaps!![0].let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                bitmaps!![1].let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            3 -> Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    bitmaps!![0].let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    bitmaps!![1].let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                bitmaps!![2].let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            else -> Icon(
                imageVector = Lucide.ListMusic,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MiuixTheme.colorScheme.primary
            )
        }
    }
}

/** 封面来源常量（与 Playlist.coverSource 对应） */
object PlaylistCoverSource {
    const val AUTO = 0
    const val SONG_COVER = 1
    const val SELF_IMAGE = 2
}

private fun loadSongCover(context: Context, songId: Long?, song: Song? = null): ImageBitmap? {
    if (songId == null) return null
    // 缓存命中直接复用（歌单封面与列表缩略图共用同一缓存）
    CoverImageCache.get(songId)?.let { return it }
    return try {
        val bitmap = if (song?.source == Song.SOURCE_DIRECTORY && song.path.isNotBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(song.path)
                retriever.embeddedPicture?.let { CoverImageDecoder.decodeScaled(it) }
            } finally {
                retriever.release()
            }
        } else {
            val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
            CoverImageDecoder.decodeScaled(context, uri)
        }
        bitmap?.let {
            val scaled = it.asImageBitmap()
            CoverImageCache.put(songId, scaled)
            scaled
        }
    } catch (_: Exception) {
        null
    }
}

private fun loadFileBitmap(context: Context, path: String): ImageBitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
