package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val BlendModes = listOf(
    "SrcOver" to "正常", "Multiply" to "正片叠底", "Screen" to "滤色",
    "Overlay" to "叠加", "SoftLight" to "柔光", "HardLight" to "强光",
    "Lighter" to "变亮", "PlusLighter" to "浅色加", "Darken" to "变暗", "PlusDarker" to "深色加"
)

/** 元素索引 → 名称 + 浅色/深色 getter/setter 映射 */
private data class ElementMapping(
    val index: Int,
    val name: String,
    val alphaLight: (AppSettingsRepository) -> Flow<Int>,
    val alphaDark: (AppSettingsRepository) -> Flow<Int>,
    val blendLight: (AppSettingsRepository) -> Flow<String>,
    val blendDark: (AppSettingsRepository) -> Flow<String>,
    val setAlphaLight: suspend AppSettingsRepository.(Int) -> Unit,
    val setAlphaDark: suspend AppSettingsRepository.(Int) -> Unit,
    val setBlendLight: suspend AppSettingsRepository.(String) -> Unit,
    val setBlendDark: suspend AppSettingsRepository.(String) -> Unit
)

private val elementMappings = listOf(
    ElementMapping(1, "拖拽手柄",
        { it.playerEl01HandleAlpha }, { it.playerEl01HandleAlphaDark },
        { it.playerEl01HandleBlend }, { it.playerEl01HandleBlendDark },
        { setPlayerEl01HandleAlpha(it) }, { setPlayerEl01HandleAlphaDark(it) },
        { setPlayerEl01HandleBlend(it) }, { setPlayerEl01HandleBlendDark(it) }
    ),
    ElementMapping(2, "歌名",
        { it.playerEl02TitleAlpha }, { it.playerEl02TitleAlphaDark },
        { it.playerEl02TitleBlend }, { it.playerEl02TitleBlendDark },
        { setPlayerEl02TitleAlpha(it) }, { setPlayerEl02TitleAlphaDark(it) },
        { setPlayerEl02TitleBlend(it) }, { setPlayerEl02TitleBlendDark(it) }
    ),
    ElementMapping(3, "专辑 - 艺术家",
        { it.playerEl03ArtistAlpha }, { it.playerEl03ArtistAlphaDark },
        { it.playerEl03ArtistBlend }, { it.playerEl03ArtistBlendDark },
        { setPlayerEl03ArtistAlpha(it) }, { setPlayerEl03ArtistAlphaDark(it) },
        { setPlayerEl03ArtistBlend(it) }, { setPlayerEl03ArtistBlendDark(it) }
    ),
    ElementMapping(4, "小歌词",
        { it.playerEl04MiniLyricAlpha }, { it.playerEl04MiniLyricAlphaDark },
        { it.playerEl04MiniLyricBlend }, { it.playerEl04MiniLyricBlendDark },
        { setPlayerEl04MiniLyricAlpha(it) }, { setPlayerEl04MiniLyricAlphaDark(it) },
        { setPlayerEl04MiniLyricBlend(it) }, { setPlayerEl04MiniLyricBlendDark(it) }
    ),
    ElementMapping(5, "进度条底轨",
        { it.playerEl05TrackAlpha }, { it.playerEl05TrackAlphaDark },
        { it.playerEl05TrackBlend }, { it.playerEl05TrackBlendDark },
        { setPlayerEl05TrackAlpha(it) }, { setPlayerEl05TrackAlphaDark(it) },
        { setPlayerEl05TrackBlend(it) }, { setPlayerEl05TrackBlendDark(it) }
    ),
    ElementMapping(6, "进度条进度",
        { it.playerEl06FillAlpha }, { it.playerEl06FillAlphaDark },
        { it.playerEl06FillBlend }, { it.playerEl06FillBlendDark },
        { setPlayerEl06FillAlpha(it) }, { setPlayerEl06FillAlphaDark(it) },
        { setPlayerEl06FillBlend(it) }, { setPlayerEl06FillBlendDark(it) }
    ),
    ElementMapping(7, "进度条时间",
        { it.playerEl07TimeAlpha }, { it.playerEl07TimeAlphaDark },
        { it.playerEl07TimeBlend }, { it.playerEl07TimeBlendDark },
        { setPlayerEl07TimeAlpha(it) }, { setPlayerEl07TimeAlphaDark(it) },
        { setPlayerEl07TimeBlend(it) }, { setPlayerEl07TimeBlendDark(it) }
    ),
    ElementMapping(8, "播控图标",
        { it.playerEl08ControlsAlpha }, { it.playerEl08ControlsAlphaDark },
        { it.playerEl08ControlsBlend }, { it.playerEl08ControlsBlendDark },
        { setPlayerEl08ControlsAlpha(it) }, { setPlayerEl08ControlsAlphaDark(it) },
        { setPlayerEl08ControlsBlend(it) }, { setPlayerEl08ControlsBlendDark(it) }
    ),
    ElementMapping(9, "播放模式",
        { it.playerEl09ModeAlpha }, { it.playerEl09ModeAlphaDark },
        { it.playerEl09ModeBlend }, { it.playerEl09ModeBlendDark },
        { setPlayerEl09ModeAlpha(it) }, { setPlayerEl09ModeAlphaDark(it) },
        { setPlayerEl09ModeBlend(it) }, { setPlayerEl09ModeBlendDark(it) }
    ),
    ElementMapping(10, "底部图标",
        { it.playerEl10BottomAlpha }, { it.playerEl10BottomAlphaDark },
        { it.playerEl10BottomBlend }, { it.playerEl10BottomBlendDark },
        { setPlayerEl10BottomAlpha(it) }, { setPlayerEl10BottomAlphaDark(it) },
        { setPlayerEl10BottomBlend(it) }, { setPlayerEl10BottomBlendDark(it) }
    )
)

/**
 * 元素配置底部弹窗
 * @param elementIndex 元素索引 1-10
 * @param onDismiss 关闭回调
 */
@Composable
fun PlayerElementConfigSheet(
    elementIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val mapping = remember(elementIndex) { elementMappings.find { it.index == elementIndex } ?: elementMappings[0] }

    val alphaLight by mapping.alphaLight(repo).collectAsState(initial = 100)
    val alphaDark by mapping.alphaDark(repo).collectAsState(initial = 100)
    val blendLight by mapping.blendLight(repo).collectAsState(initial = "SrcOver")
    val blendDark by mapping.blendDark(repo).collectAsState(initial = "SrcOver")

    val blendLabelLight = BlendModes.find { it.first == blendLight }?.second ?: blendLight
    val blendLabelDark = BlendModes.find { it.first == blendDark }?.second ?: blendDark

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = mapping.name
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // 浅色模式
            Text(
                text = "浅色模式",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            // 透明度
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "透明度", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.weight(1f))
                Text(text = "${alphaLight}%", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Slider(value = alphaLight.toFloat(), onValueChange = { scope.launch { mapping.setAlphaLight(repo, it.toInt()) } }, valueRange = 0f..100f, steps = 99)
            // 混合模式
            OverlayDropdownMenu(
                entries = listOf(DropdownEntry(
                    items = BlendModes.map { (key, label) ->
                        DropdownItem(text = label, selected = blendLight == key, onClick = { scope.launch { mapping.setBlendLight(repo, key) } })
                    }
                )),
                title = "混合模式",
                summary = blendLabelLight
            )

            // 深色模式
            Text(
                text = "深色模式",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "透明度", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.weight(1f))
                Text(text = "${alphaDark}%", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Slider(value = alphaDark.toFloat(), onValueChange = { scope.launch { mapping.setAlphaDark(repo, it.toInt()) } }, valueRange = 0f..100f, steps = 99)
            OverlayDropdownMenu(
                entries = listOf(DropdownEntry(
                    items = BlendModes.map { (key, label) ->
                        DropdownItem(text = label, selected = blendDark == key, onClick = { scope.launch { mapping.setBlendDark(repo, key) } })
                    }
                )),
                title = "混合模式",
                summary = blendLabelDark
            )
        }
    }
}
