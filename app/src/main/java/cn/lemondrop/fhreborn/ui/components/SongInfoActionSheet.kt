package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User

/**
 * 歌曲信息动作面板。
 *
 * 从播放器歌曲信息区域弹出，列出当前歌曲的所有艺术家与专辑，
 * 点击艺术家/专辑可关闭面板并导航到对应详情页。
 *
 * @param visible 是否显示
 * @param artists 艺术家列表（已按当前分隔符拆分）
 * @param album 专辑名
 * @param onDismiss 关闭回调
 * @param onArtistClick 点击某个艺术家
 * @param onAlbumClick 点击专辑
 */
@Composable
fun SongInfoActionSheet(
    visible: Boolean,
    artists: List<String>,
    album: String,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: () -> Unit
) {
    if (!visible) return

    FhBottomSheet(
        show = visible,
        onDismissRequest = onDismiss,
        title = "歌曲信息",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            artists.forEach { artist ->
                FhListItem(
                    title = artist,
                    summary = "艺术家",
                    leading = {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    },
                    onClick = { onArtistClick(artist) }
                )
            }

            FhListItem(
                title = album,
                summary = "专辑",
                leading = {
                    Icon(
                        imageVector = Lucide.Disc,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                },
                onClick = onAlbumClick
            )
        }
    }
}
