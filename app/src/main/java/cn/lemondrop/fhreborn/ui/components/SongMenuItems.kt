package cn.lemondrop.fhreborn.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.DiscAlbum
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.ListMinus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Trash2

/**
 * 菜单项定义：文案 + 图标 + 是否危险操作。
 * 播放器菜单 / 曲目菜单 / 歌单菜单共用同一套定义，保证图标与说明文字全局一致。
 */
data class SongMenuItem(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false
)

object SongMenuItems {
    val PlayNext = SongMenuItem("下一首播放", Lucide.SkipForward)
    val AddToPlaylist = SongMenuItem("加入歌单", Lucide.Plus)
    val Thoughts = SongMenuItem("想法", Lucide.Lightbulb)
    val ViewAlbum = SongMenuItem("查看专辑", Lucide.DiscAlbum)
    val ViewArtist = SongMenuItem("查看艺术家", Lucide.UserRound)
    val GoToFolder = SongMenuItem("转至文件夹", Lucide.FolderOpen)
    val Share = SongMenuItem("分享文件", Lucide.Share2)
    val OpenWith = SongMenuItem("用其他 app 打开", Lucide.ExternalLink)
    val Properties = SongMenuItem("属性", Lucide.Info)
    val Hide = SongMenuItem("隐藏音乐", Lucide.EyeOff)
    val Delete = SongMenuItem("删除文件", Lucide.Trash2, destructive = true)
    val RemoveFromPlaylist = SongMenuItem("从歌单移除", Lucide.ListMinus, destructive = true)
}
