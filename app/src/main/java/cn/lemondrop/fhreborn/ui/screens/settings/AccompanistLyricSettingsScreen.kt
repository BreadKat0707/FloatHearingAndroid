package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Accompanist Lyric 设置内容（纯内容组件，不带外壳）。
 * 提供歌词文字大小、粗细、翻译/音标显示、非当前行模糊、对齐方式等配置。
 */
@Composable
fun AccompanistLyricSettingsContent(
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val mainTextSize by repository.acclLyricMainTextSizeSp.collectAsState(initial = 34)
    val accompanimentTextSize by repository.acclLyricAccompanimentTextSizeSp.collectAsState(initial = 20)
    val phoneticTextSize by repository.acclLyricPhoneticTextSizeSp.collectAsState(initial = 13)
    val mainFontWeight by repository.acclLyricMainFontWeight.collectAsState(initial = 700)
    val accompanimentFontWeight by repository.acclLyricAccompanimentFontWeight.collectAsState(initial = 700)
    val phoneticFontWeight by repository.acclLyricPhoneticFontWeight.collectAsState(initial = 400)
    val showTranslation by repository.acclLyricShowTranslation.collectAsState(initial = true)
    val showPhonetic by repository.acclLyricShowPhonetic.collectAsState(initial = true)
    val useBlur by repository.acclLyricUseBlurEffect.collectAsState(initial = true)
    val blurDelta by repository.acclLyricBlurDelta.collectAsState(initial = 3)
    val textAlign by repository.acclLyricTextAlign.collectAsState(initial = "center")
    val glowEffect by repository.acclLyricGlowEffect.collectAsState(initial = true)
    val breathingDotsSize by repository.acclLyricBreathingDotsSize.collectAsState(initial = 16)
    val translationTextSize by repository.acclLyricTranslationTextSizeSp.collectAsState(initial = 14)
    val translationFontWeight by repository.acclLyricTranslationFontWeight.collectAsState(initial = 400)
    val linePositionPercent by repository.acclLyricLinePositionPercent.collectAsState(initial = 35)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(
            bottom = bottomOverlayHeight + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 主唱行
        item { SectionHeader("主唱行") }
        item {
            SliderSettingItem(
                title = "文字大小",
                value = mainTextSize,
                range = 16f..48f,
                steps = 31,
                valueText = "${mainTextSize}sp",
                onValueChange = { scope.launch { repository.setAcclLyricMainTextSizeSp(it.toInt()) } }
            )
        }
        item {
            SliderSettingItem(
                title = "文字粗细",
                value = mainFontWeight,
                range = 100f..900f,
                steps = 7,
                valueText = "$mainFontWeight",
                keyPoints = listOf(100f, 200f, 300f, 400f, 500f, 600f, 700f, 800f, 900f),
                onValueChange = { scope.launch { repository.setAcclLyricMainFontWeight(it.toInt()) } }
            )
        }

        // 伴唱行
        item { SectionHeader("伴唱行") }
        item {
            SliderSettingItem(
                title = "文字大小",
                value = accompanimentTextSize,
                range = 12f..32f,
                steps = 19,
                valueText = "${accompanimentTextSize}sp",
                onValueChange = { scope.launch { repository.setAcclLyricAccompanimentTextSizeSp(it.toInt()) } }
            )
        }
        item {
            SliderSettingItem(
                title = "文字粗细",
                value = accompanimentFontWeight,
                range = 100f..900f,
                steps = 7,
                valueText = "$accompanimentFontWeight",
                keyPoints = listOf(100f, 200f, 300f, 400f, 500f, 600f, 700f, 800f, 900f),
                onValueChange = { scope.launch { repository.setAcclLyricAccompanimentFontWeight(it.toInt()) } }
            )
        }

        // 音标/注音
        item { SectionHeader("音标 / 注音") }
        item {
            SliderSettingItem(
                title = "文字大小",
                value = phoneticTextSize,
                range = 8f..24f,
                steps = 15,
                valueText = "${phoneticTextSize}sp",
                onValueChange = { scope.launch { repository.setAcclLyricPhoneticTextSizeSp(it.toInt()) } }
            )
        }
        item {
            SliderSettingItem(
                title = "文字粗细",
                value = phoneticFontWeight,
                range = 100f..900f,
                steps = 7,
                valueText = "$phoneticFontWeight",
                keyPoints = listOf(100f, 200f, 300f, 400f, 500f, 600f, 700f, 800f, 900f),
                onValueChange = { scope.launch { repository.setAcclLyricPhoneticFontWeight(it.toInt()) } }
            )
        }

        // 显示开关
        item { SectionHeader("显示") }
        item {
            ToggleSettingItem(
                title = "显示翻译",
                checked = showTranslation,
                onCheckedChange = { scope.launch { repository.setAcclLyricShowTranslation(it) } }
            )
        }
        item {
            ToggleSettingItem(
                title = "显示音标 / 注音",
                checked = showPhonetic,
                onCheckedChange = { scope.launch { repository.setAcclLyricShowPhonetic(it) } }
            )
        }
        item {
            SliderSettingItem(
                title = "翻译文字大小",
                value = translationTextSize,
                range = 10f..28f,
                steps = 17,
                valueText = "${translationTextSize}sp",
                enabled = showTranslation,
                onValueChange = { scope.launch { repository.setAcclLyricTranslationTextSizeSp(it.toInt()) } }
            )
        }
        item {
            SliderSettingItem(
                title = "翻译文字粗细",
                value = translationFontWeight,
                range = 100f..900f,
                steps = 7,
                valueText = "$translationFontWeight",
                enabled = showTranslation,
                keyPoints = listOf(100f, 200f, 300f, 400f, 500f, 600f, 700f, 800f, 900f),
                onValueChange = { scope.launch { repository.setAcclLyricTranslationFontWeight(it.toInt()) } }
            )
        }

        // 效果
        item { SectionHeader("效果") }
        item {
            ToggleSettingItem(
                title = "非当前行模糊效果",
                checked = useBlur,
                onCheckedChange = { scope.launch { repository.setAcclLyricUseBlurEffect(it) } }
            )
        }
        item {
            SliderSettingItem(
                title = "模糊强度",
                value = blurDelta,
                range = 0f..10f,
                steps = 9,
                valueText = "$blurDelta",
                enabled = useBlur,
                onValueChange = { scope.launch { repository.setAcclLyricBlurDelta(it.toInt()) } }
            )
        }
        item {
            ToggleSettingItem(
                title = "歌词发光效果",
                summary = "深色模式叠加发光混合，浅色模式加深对比",
                checked = glowEffect,
                onCheckedChange = { scope.launch { repository.setAcclLyricGlowEffect(it) } }
            )
        }
        item {
            SliderSettingItem(
                title = "呼吸点大小",
                value = breathingDotsSize,
                range = 8f..32f,
                steps = 23,
                valueText = "${breathingDotsSize}dp",
                onValueChange = { scope.launch { repository.setAcclLyricBreathingDotsSize(it.toInt()) } }
            )
        }

        // 当前行位置
        item { SectionHeader("当前行位置") }
        item {
            SliderSettingItem(
                title = "竖向位置",
                summary = "当前行歌词中心在视口中的高度占比",
                value = linePositionPercent,
                range = 10f..90f,
                steps = 15,
                valueText = "$linePositionPercent%",
                onValueChange = { scope.launch { repository.setAcclLyricLinePositionPercent(it.toInt()) } }
            )
        }

        // 对齐方式：当前 Accompanist Lyric 版本不支持全局强制对齐，仅跟随歌词本身标注
        item { SectionHeader("对齐方式") }
        item {
            Text(
                text = "当前 Accompanist Lyric 版本不支持全局歌词对齐，仅跟随歌词本身标注。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
    // 滚动条：自动淡入淡出，可拖动定位
    LazyListScrollBar(
        listState = listState,
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    }
}

@Composable
private fun SectionHeader(title: String) {
    // 使用 miuix 默认 SmallTitle（不自定义颜色）
    top.yukonga.miuix.kmp.basic.SmallTitle(text = title)
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    enabled: Boolean = true,
    summary: String? = null,
    keyPoints: List<Float>? = null,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = valueText,
                style = MiuixTheme.textStyles.body2,
                color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        if (summary != null) {
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            keyPoints = keyPoints,
            showKeyPoints = keyPoints != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleSettingItem(
    title: String,
    checked: Boolean,
    summary: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    top.yukonga.miuix.kmp.preference.SwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = title,
        summary = summary,
    )
}

@Composable
private fun SelectionSettingItem(
    title: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        FhBottomSheet(
            show = true,
            onDismissRequest = { showDialog = false },
            title = title,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            content = {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLabel == label,
                            onClick = {
                                onSelected(key)
                                showDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label)
                    }
                }
            }
        )
    }

    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = title,
        onClick = { showDialog = true },
    )
}