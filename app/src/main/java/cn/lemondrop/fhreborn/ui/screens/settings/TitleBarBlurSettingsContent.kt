package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TitleBarBlurSettingsContent(
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val titleBarStyle by settings.titleBarStyle.collectAsState(initial = "gaussian")
    val radius by settings.titleBarBlurRadius.collectAsState(initial = 10)
    val startFraction by settings.titleBarProgressiveStart.collectAsState(initial = 0)
    val endFraction by settings.titleBarProgressiveEnd.collectAsState(initial = 100)
    val curve by settings.titleBarProgressiveCurve.collectAsState(initial = 220)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    observeSettingsScroll(listState, onScrolledChange)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = bottomOverlayHeight + 16.dp
            )
        ) {
            item {
                Text(
                    text = if (titleBarStyle == "progressive") {
                        "当前标题栏使用 Progressive Blur，以下参数实时生效"
                    } else {
                        "当前标题栏使用 Gaussian，切换 Progressive 后以下参数生效"
                    },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            item { SmallTitle("模糊强度") }
            item {
                SliderPreference(
                    title = "模糊半径",
                    summary = "0 - 150 dp",
                    value = radius.toFloat(),
                    onValueChange = {
                        scope.launch { settings.setTitleBarBlurRadius(it.toInt()) }
                    },
                    valueText = "$radius dp",
                    valueRange = 0f..150f,
                    steps = 149
                )
            }

            item { SmallTitle("渐变范围") }
            item {
                SliderPreference(
                    title = "强模糊起点",
                    summary = "渐变色带中模糊保持满强度的位置",
                    value = startFraction.toFloat(),
                    onValueChange = {
                        scope.launch { settings.setTitleBarProgressiveStart(it.toInt()) }
                    },
                    valueText = "$startFraction%",
                    valueRange = 0f..100f,
                    steps = 100
                )
            }
            item {
                SliderPreference(
                    title = "清晰结束点",
                    summary = "渐变色带中模糊衰减到零的位置",
                    value = endFraction.toFloat(),
                    onValueChange = {
                        scope.launch { settings.setTitleBarProgressiveEnd(it.toInt()) }
                    },
                    valueText = "$endFraction%",
                    valueRange = 0f..100f,
                    steps = 100
                )
            }
            item {
                SliderPreference(
                    title = "渐变曲线",
                    summary = "低于 1 偏向模糊端，高于 1 偏向清晰端",
                    value = curve.toFloat(),
                    onValueChange = {
                        scope.launch { settings.setTitleBarProgressiveCurve(it.toInt()) }
                    },
                    valueText = String.format("%.1fx", curve / 100f),
                    valueRange = 20f..300f,
                    steps = 27
                )
            }
        }
        LazyListScrollBar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
            trackPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = bottomOverlayHeight
            )
        )
    }
}
