package cn.lemondrop.fhreborn.ui.screens.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverButton
import cn.lemondrop.clover.CloverCheckbox
import cn.lemondrop.clover.CloverCircularProgressIndicator
import cn.lemondrop.clover.CloverColors
import cn.lemondrop.clover.CloverDialog
import cn.lemondrop.clover.CloverDropdownMenu
import cn.lemondrop.clover.CloverDropdownMenuItem
import cn.lemondrop.clover.CloverEmptyList
import cn.lemondrop.clover.CloverFlyout
import cn.lemondrop.clover.CloverHorizontalListItem
import cn.lemondrop.clover.CloverIconButton
import cn.lemondrop.clover.CloverLinearProgressIndicator
import cn.lemondrop.clover.CloverListItem
import cn.lemondrop.clover.CloverListSkeleton
import cn.lemondrop.clover.CloverOutlinedButton
import cn.lemondrop.clover.CloverRadioButton
import cn.lemondrop.clover.CloverRevealHost
import cn.lemondrop.clover.CloverSectionHeader
import cn.lemondrop.clover.CloverSettingItem
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.CloverSlider
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.clover.CloverSwitch
import cn.lemondrop.clover.CloverSwitchItem
import cn.lemondrop.clover.CloverTextButton
import cn.lemondrop.clover.CloverTheme
import cn.lemondrop.clover.cloverRevealItem
import cn.lemondrop.clover.isCloverDark
import io.github.composefluent.component.Text

@Composable
fun CloverDemoScreen(
    onBack: () -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

    val initialDark = isCloverDark()
    var isDemoDark by remember { mutableStateOf(initialDark) }

    var checked1 by remember { mutableStateOf(true) }
    var checked2 by remember { mutableStateOf(false) }
    var radioSelected by remember { mutableStateOf(0) }
    var sliderValue by remember { mutableStateOf(0.4f) }
    var progressValue by remember { mutableStateOf(0.6f) }
    var showDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    var showFlyout by remember { mutableStateOf(false) }

    CloverTheme(darkTheme = isDemoDark) {
        val isDark = isCloverDark()
        val bgColor = if (isDark) CloverColors.backgroundDark else CloverColors.backgroundLight

        var selectedSongIndex by remember { mutableIntStateOf(2) }
        var playingSongIndex by remember { mutableIntStateOf(1) }
        var switchState by remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
            CloverRevealHost(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarPadding,
                        start = CloverSizes.listOuterHorizontalPadding,
                        end = CloverSizes.listOuterHorizontalPadding
                    )
                ) {
                item {
                    CloverSwitchItem(
                        modifier = Modifier.cloverRevealItem(),
                        title = "深色模式",
                        subtitle = "仅影响此 Demo 页面",
                        checked = isDemoDark,
                        onCheckedChange = { isDemoDark = it }
                    )
                }

                item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CloverSizes.listOuterHorizontalPadding, vertical = CloverSpacing.lg)
                ) {
                    Text(
                        text = "Clover Design",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isDark) CloverColors.onSurfaceDark else CloverColors.onSurfaceLight
                    )
                    Text(
                        text = "列表组件样式演示",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight
                    )
                }
            }

            item { CloverSectionHeader(title = "正在播放 · ${playingSongIndex + 1} / ${sampleSongs.size}") }
            items(sampleSongs.size) { index ->
                val song = sampleSongs[index]
                CloverListItem(
                    modifier = Modifier.cloverRevealItem(),
                    title = song.title,
                    subtitle = "${song.artist} · ${song.album}",
                    isSelected = index == selectedSongIndex,
                    isPlaying = index == playingSongIndex,
                    onClick = {
                        selectedSongIndex = index
                        playingSongIndex = index
                    },
                    onLongClick = { /* 长按菜单 */ },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(CloverSizes.coverSmall)
                                .clip(RoundedCornerShape(CloverSizes.coverCornerRadius))
                                .background(song.coverColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Music,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(CloverSizes.iconSmall)
                            )
                        }
                    },
                    trailing = {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "更多",
                                tint = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight,
                                modifier = Modifier.size(CloverSizes.iconSmall)
                            )
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "艺术家") }
            items(sampleArtists.size) { index ->
                val artist = sampleArtists[index]
                CloverListItem(
                    modifier = Modifier.cloverRevealItem(),
                    title = artist.name,
                    subtitle = "${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲",
                    onClick = { },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(CloverSizes.coverSmall)
                                .clip(RoundedCornerShape(CloverSizes.avatarCornerRadius))
                                .background(if (isDark) CloverColors.surfaceVariantDark else CloverColors.surfaceVariantLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = artist.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight
                            )
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "该艺术家的专辑") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CloverSizes.listOuterHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                ) {
                    items(sampleArtistAlbums.size) { index ->
                        val album = sampleArtistAlbums[index]
                        CloverHorizontalListItem(
                            modifier = Modifier.cloverRevealItem(),
                            title = album.title,
                            subtitle = album.year,
                            onClick = { },
                            cover = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(album.coverColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Music,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(CloverSizes.iconMedium)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "参与制作的艺术家") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CloverSizes.listOuterHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                ) {
                    items(sampleAlbumArtists.size) { index ->
                        val artist = sampleAlbumArtists[index]
                        CloverHorizontalListItem(
                            modifier = Modifier.cloverRevealItem(),
                            title = artist.name,
                            subtitle = artist.role,
                            onClick = { },
                            cover = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isDark) CloverColors.surfaceVariantDark
                                            else CloverColors.surfaceVariantLight
                                        )
                                        .clip(RoundedCornerShape(CloverSizes.avatarCornerRadius)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = artist.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isDark) CloverColors.onSurfaceVariantDark
                                        else CloverColors.onSurfaceVariantLight
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "设置") }
            item {
                CloverListItem(
                    modifier = Modifier.cloverRevealItem(),
                    title = "深色模式",
                    subtitle = "跟随系统",
                    onClick = { switchState = !switchState },
                    leading = {
                        Icon(
                            imageVector = Lucide.Palette,
                            contentDescription = null,
                            tint = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight,
                            modifier = Modifier.size(CloverSizes.iconMedium)
                        )
                    },
                    trailing = {
                        CloverSwitch(
                            checked = switchState,
                            onCheckedChange = { switchState = it }
                        )
                    }
                )
            }
            item {
                CloverListItem(
                    modifier = Modifier.cloverRevealItem(),
                    title = "关于",
                    subtitle = "查看版本和致谢",
                    onClick = { },
                    leading = {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = null,
                            tint = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight,
                            modifier = Modifier.size(CloverSizes.iconMedium)
                        )
                    },
                    trailing = {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "CloverButton") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                ) {
                    CloverButton(
                        text = "实心",
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                    CloverOutlinedButton(
                        text = "描边",
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CloverTextButton(text = "文字按钮", onClick = { })
                    CloverIconButton(
                        icon = Lucide.Music,
                        contentDescription = "播放",
                        onClick = { }
                    )
                    CloverButton(
                        text = "大号",
                        onClick = { },
                        size = cn.lemondrop.clover.CloverButtonSize.Large
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "基础控件") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Switch")
                    CloverSwitch(
                        checked = checked1,
                        onCheckedChange = { checked1 = it }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Checkbox")
                    CloverCheckbox(
                        checked = checked2,
                        onCheckedChange = { checked2 = it }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CloverSpacing.sm)
                ) {
                    Text("Radio Group")
                    listOf("选项 A", "选项 B", "选项 C").forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { radioSelected = index },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                        ) {
                            CloverRadioButton(
                                selected = radioSelected == index,
                                onClick = { radioSelected = index }
                            )
                            Text(label)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Slider: ${(sliderValue * 100).toInt()}%")
                    CloverSlider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                ) {
                    CloverLinearProgressIndicator(progress = progressValue)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
                    ) {
                        CloverCircularProgressIndicator(progress = progressValue, size = 40.dp)
                        CloverCircularProgressIndicator(progress = null, size = 32.dp)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CloverButton(text = "打开 Dialog", onClick = { showDialog = true })
                }
                if (showDialog) {
                    CloverDialog(
                        onDismissRequest = { showDialog = false },
                        title = "确认操作",
                        buttons = {
                            CloverTextButton(
                                text = "取消",
                                onClick = { showDialog = false },
                                reveal = false
                            )
                            CloverButton(
                                text = "确认",
                                onClick = { showDialog = false },
                                reveal = false
                            )
                        }
                    ) {
                        Text("这是一个 Clover Dialog 示例。")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CloverButton(text = "打开 Dropdown", onClick = { expandedMenu = true })
                    CloverDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        CloverDropdownMenuItem(
                            text = "菜单项 1",
                            onClick = { expandedMenu = false }
                        )
                        CloverDropdownMenuItem(
                            text = "菜单项 2",
                            onClick = { expandedMenu = false }
                        )
                        CloverDropdownMenuItem(
                            text = "菜单项 3",
                            onClick = { expandedMenu = false }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(CloverSpacing.md)) }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CloverButton(text = "打开 Flyout", onClick = { showFlyout = true })
                }
                if (showFlyout) {
                    CloverFlyout(
                        visible = showFlyout,
                        onDismiss = { showFlyout = false }
                    ) {
                        Text("Flyout 内容")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "加载中") }
            item { CloverListSkeleton(count = 4) }

            item { Spacer(modifier = Modifier.height(CloverSpacing.lg)) }

            item { CloverSectionHeader(title = "空状态") }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    CloverEmptyList(
                        title = "还没有歌曲",
                        message = "去扫描本地音乐，或者添加一个歌单吧",
                        icon = Lucide.ListMusic,
                        actionLabel = "去扫描",
                        onActionClick = { }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(navBarPadding + CloverSpacing.xl)) }
        }
        }

        // 返回按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding + CloverSpacing.lg)
        ) {
            Text(
                text = "点击返回",
                color = if (isDark) CloverColors.onSurfaceVariantDark else CloverColors.onSurfaceVariantLight,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
    }
}

private val sampleSongs = listOf(
    DemoSong("Midnight City", "M83", "Hurry Up, We're Dreaming", Color(0xFF7C4DFF)),
    DemoSong("Blinding Lights", "The Weeknd", "After Hours", Color(0xFFFF5252)),
    DemoSong("Levitating", "Dua Lipa", "Future Nostalgia", Color(0xFF448AFF)),
    DemoSong("Peaches", "Justin Bieber", "Justice", Color(0xFFFFAB40)),
    DemoSong("Save Your Tears", "The Weeknd", "After Hours", Color(0xFFFF5252)),
    DemoSong("Good 4 U", "Olivia Rodrigo", "SOUR", Color(0xFF69F0AE))
)

private val sampleArtists = listOf(
    DemoArtist("The Weeknd", 5, 42),
    DemoArtist("Dua Lipa", 2, 28),
    DemoArtist("Olivia Rodrigo", 1, 15),
    DemoArtist("M83", 7, 68)
)

private val sampleArtistAlbums = listOf(
    DemoAlbum("After Hours", "2020", Color(0xFFFF5252)),
    DemoAlbum("Starboy", "2016", Color(0xFF7C4DFF)),
    DemoAlbum("Beauty Behind the Madness", "2015", Color(0xFF448AFF)),
    DemoAlbum("Dawn FM", "2022", Color(0xFFFFAB40))
)

private val sampleAlbumArtists = listOf(
    DemoContributor("The Weeknd", "演唱"),
    DemoContributor("Max Martin", "制作"),
    DemoContributor("Oscar Holter", "作曲"),
    DemoContributor("Oneohtrix Point Never", "编曲")
)

private data class DemoSong(
    val title: String,
    val artist: String,
    val album: String,
    val coverColor: Color
)

private data class DemoArtist(
    val name: String,
    val albumCount: Int,
    val songCount: Int
)

private data class DemoAlbum(
    val title: String,
    val year: String,
    val coverColor: Color
)

private data class DemoContributor(
    val name: String,
    val role: String
)
