package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val BlendModes = listOf(
    "SrcOver" to "正常", "Multiply" to "正片叠底", "Screen" to "滤色",
    "Overlay" to "叠加", "SoftLight" to "柔光", "HardLight" to "强光",
    "Lighter" to "变亮", "PlusLighter" to "浅色加", "Darken" to "变暗", "PlusDarker" to "深色加"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerElementAppearanceContent(paddingValues: PaddingValues, bottomOverlayHeight: Dp) {
    val context = LocalContext.current
    val repo = remember { AppSettingsRepository(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(bottom = bottomOverlayHeight + 16.dp)
    ) {
        item {
            TabRowWithContour(
                tabs = listOf("浅色模式", "深色模式"),
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        val isDark = selectedTab == 1

        // 1. 拖拽手柄
        item { El("拖拽手柄", isDark,
            a = { if (isDark) repo.playerEl01HandleAlphaDark else repo.playerEl01HandleAlpha },
            b = { if (isDark) repo.playerEl01HandleBlendDark else repo.playerEl01HandleBlend },
            sa = { if (isDark) repo.setPlayerEl01HandleAlphaDark(it) else repo.setPlayerEl01HandleAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl01HandleBlendDark(it) else repo.setPlayerEl01HandleBlend(it) }
        ) }
        // 2. 歌名
        item { El("歌名", isDark,
            a = { if (isDark) repo.playerEl02TitleAlphaDark else repo.playerEl02TitleAlpha },
            b = { if (isDark) repo.playerEl02TitleBlendDark else repo.playerEl02TitleBlend },
            sa = { if (isDark) repo.setPlayerEl02TitleAlphaDark(it) else repo.setPlayerEl02TitleAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl02TitleBlendDark(it) else repo.setPlayerEl02TitleBlend(it) }
        ) }
        // 3. 专辑-艺术家
        item { El("专辑 - 艺术家", isDark,
            a = { if (isDark) repo.playerEl03ArtistAlphaDark else repo.playerEl03ArtistAlpha },
            b = { if (isDark) repo.playerEl03ArtistBlendDark else repo.playerEl03ArtistBlend },
            sa = { if (isDark) repo.setPlayerEl03ArtistAlphaDark(it) else repo.setPlayerEl03ArtistAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl03ArtistBlendDark(it) else repo.setPlayerEl03ArtistBlend(it) }
        ) }
        // 4. 小歌词
        item { El("小歌词", isDark,
            a = { if (isDark) repo.playerEl04MiniLyricAlphaDark else repo.playerEl04MiniLyricAlpha },
            b = { if (isDark) repo.playerEl04MiniLyricBlendDark else repo.playerEl04MiniLyricBlend },
            sa = { if (isDark) repo.setPlayerEl04MiniLyricAlphaDark(it) else repo.setPlayerEl04MiniLyricAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl04MiniLyricBlendDark(it) else repo.setPlayerEl04MiniLyricBlend(it) }
        ) }
        // 5. 进度条底轨
        item { El("进度条底轨", isDark,
            a = { if (isDark) repo.playerEl05TrackAlphaDark else repo.playerEl05TrackAlpha },
            b = { if (isDark) repo.playerEl05TrackBlendDark else repo.playerEl05TrackBlend },
            sa = { if (isDark) repo.setPlayerEl05TrackAlphaDark(it) else repo.setPlayerEl05TrackAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl05TrackBlendDark(it) else repo.setPlayerEl05TrackBlend(it) }
        ) }
        // 6. 进度条进度
        item { El("进度条进度", isDark,
            a = { if (isDark) repo.playerEl06FillAlphaDark else repo.playerEl06FillAlpha },
            b = { if (isDark) repo.playerEl06FillBlendDark else repo.playerEl06FillBlend },
            sa = { if (isDark) repo.setPlayerEl06FillAlphaDark(it) else repo.setPlayerEl06FillAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl06FillBlendDark(it) else repo.setPlayerEl06FillBlend(it) }
        ) }
        // 7. 进度条时间文字
        item { El("进度条时间", isDark,
            a = { if (isDark) repo.playerEl07TimeAlphaDark else repo.playerEl07TimeAlpha },
            b = { if (isDark) repo.playerEl07TimeBlendDark else repo.playerEl07TimeBlend },
            sa = { if (isDark) repo.setPlayerEl07TimeAlphaDark(it) else repo.setPlayerEl07TimeAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl07TimeBlendDark(it) else repo.setPlayerEl07TimeBlend(it) }
        ) }
        // 8. 播控图标
        item { El("播控图标", isDark,
            a = { if (isDark) repo.playerEl08ControlsAlphaDark else repo.playerEl08ControlsAlpha },
            b = { if (isDark) repo.playerEl08ControlsBlendDark else repo.playerEl08ControlsBlend },
            sa = { if (isDark) repo.setPlayerEl08ControlsAlphaDark(it) else repo.setPlayerEl08ControlsAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl08ControlsBlendDark(it) else repo.setPlayerEl08ControlsBlend(it) }
        ) }
        // 9. 播放模式
        item { El("播放模式", isDark,
            a = { if (isDark) repo.playerEl09ModeAlphaDark else repo.playerEl09ModeAlpha },
            b = { if (isDark) repo.playerEl09ModeBlendDark else repo.playerEl09ModeBlend },
            sa = { if (isDark) repo.setPlayerEl09ModeAlphaDark(it) else repo.setPlayerEl09ModeAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl09ModeBlendDark(it) else repo.setPlayerEl09ModeBlend(it) }
        ) }
        // 10. 底部图标
        item { El("底部图标", isDark,
            a = { if (isDark) repo.playerEl10BottomAlphaDark else repo.playerEl10BottomAlpha },
            b = { if (isDark) repo.playerEl10BottomBlendDark else repo.playerEl10BottomBlend },
            sa = { if (isDark) repo.setPlayerEl10BottomAlphaDark(it) else repo.setPlayerEl10BottomAlpha(it) },
            sb = { if (isDark) repo.setPlayerEl10BottomBlendDark(it) else repo.setPlayerEl10BottomBlend(it) }
        ) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun El(
    title: String, isDark: Boolean,
    a: () -> Flow<Int>, b: () -> Flow<String>,
    sa: suspend (Int) -> Unit, sb: suspend (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val flow = a()
    val blendFlow = b()
    val alpha by flow.collectAsState(initial = 100)
    val blendMode by blendFlow.collectAsState(initial = "SrcOver")
    val blendLabel = BlendModes.find { it.first == blendMode }?.second ?: blendMode
    var dropdownExpanded by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
            // 透明度
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "透明度", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.weight(1f))
                Text(text = "${alpha}%", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Slider(value = alpha.toFloat(), onValueChange = { scope.launch { sa(it.toInt()) } }, valueRange = 0f..100f, steps = 99)
            // 混合模式 (Spinner)
            OverlayDropdownMenu(
                entries = listOf(
                    DropdownEntry(
                        items = BlendModes.map { (key, label) ->
                            DropdownItem(text = label, selected = blendMode == key, onClick = { scope.launch { sb(key) } })
                        }
                    )
                ),
                title = "混合模式",
                summary = blendLabel
            )
        }
    }
}
