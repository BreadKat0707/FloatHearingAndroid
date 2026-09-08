package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.viewmodel.SettingsViewModel
import cn.lemondrop.fhreborn.util.BackgroundImageUtils
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val KEY_BG_TYPE = "bg_type"
private const val KEY_BG_FOREGROUND = "bg_foreground"
private const val KEY_BG_COLOR = "bg_color"
private const val KEY_BG_IMAGE_PATH = "bg_image_path"
private const val KEY_BG_IMAGE_BRIGHTNESS = "bg_image_brightness"
private const val KEY_BG_IMAGE_BLUR = "bg_image_blur"

// 纯色预设色板：label -> hex（空 hex = 跟随主题背景色）
private val PRESET_COLORS = listOf(
    "主题色" to "",
    "白" to "#FFFFFFFF",
    "浅灰" to "#FFEEEEEE",
    "黑" to "#FF000000",
    "深灰" to "#FF1C1C1E",
    "红" to "#FFF44336",
    "橙" to "#FFFF9800",
    "绿" to "#FF4CAF50",
    "蓝" to "#FF2196F3",
    "靛" to "#FF3F51B5",
    "紫" to "#FF9C27B0",
    "粉" to "#FFE91E63",
    "蓝灰" to "#FF607D8B"
)

/**
 * 主页面背景设置子页内容（作为 SettingsScreen 的 content 子视图使用，不自带外壳）。
 *
 * 二选一：纯色 / 自选图片（亮度 + 模糊）。
 */
@Composable
fun BackgroundSettingsContent(
    viewModel: SettingsViewModel,
    paddingValues: PaddingValues = PaddingValues(),
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgType by viewModel.getStringValue(KEY_BG_TYPE, "color").collectAsState(initial = "color")
    val bgForeground by viewModel.getStringValue(KEY_BG_FOREGROUND, "auto").collectAsState(initial = "auto")
    val bgColor by viewModel.getStringValue(KEY_BG_COLOR, "").collectAsState(initial = "")
    val bgImagePath by viewModel.getStringValue(KEY_BG_IMAGE_PATH, "").collectAsState(initial = "")
    val bgImageBrightness by viewModel.getIntValue(KEY_BG_IMAGE_BRIGHTNESS, 100).collectAsState(initial = 100)
    val bgImageBlur by viewModel.getIntValue(KEY_BG_IMAGE_BLUR, 0).collectAsState(initial = 0)

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = BackgroundImageUtils.copyImageToInternal(context, uri)
            if (path != null) {
                viewModel.setStringSetting(KEY_BG_IMAGE_PATH, path)
            }
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value) {
        onScrolledChange(scrollState.value > 0)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
        SectionTitle("背景类型")

        TypeOption("纯色", selected = bgType == "color") {
            viewModel.setStringSetting(KEY_BG_TYPE, "color")
        }
        TypeOption("自选图片", selected = bgType == "image") {
            viewModel.setStringSetting(KEY_BG_TYPE, "image")
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionTitle("前景色")
        TypeOption("跟随颜色模式", selected = bgForeground == "auto") {
            viewModel.setStringSetting(KEY_BG_FOREGROUND, "auto")
        }
        TypeOption("浅色（适合深色背景）", selected = bgForeground == "light") {
            viewModel.setStringSetting(KEY_BG_FOREGROUND, "light")
        }
        TypeOption("深色（适合浅色背景）", selected = bgForeground == "dark") {
            viewModel.setStringSetting(KEY_BG_FOREGROUND, "dark")
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (bgType) {
            "color" -> {
                SectionTitle("预设颜色")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PRESET_COLORS.forEach { (label, hex) ->
                        ColorSwatch(
                            label = label,
                            swatchColor = if (hex.isBlank()) {
                                MiuixTheme.colorScheme.background
                            } else {
                                Color(android.graphics.Color.parseColor(hex))
                            },
                            selected = bgColor == hex,
                            onClick = { viewModel.setStringSetting(KEY_BG_COLOR, hex) }
                        )
                    }
                }
            }

            "image" -> {
                SectionTitle("自选图片")
                val previewBitmap = remember(bgImagePath) {
                    BackgroundImageUtils.loadBitmapFromPath(bgImagePath)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                        .clickable {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(bgImageBlur.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (100 - bgImageBrightness) / 100f * 0.85f))
                        )
                    } else {
                        Text(
                            text = "点击选择图片",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (previewBitmap != null) "点击预览可重新选择图片" else "尚未选择图片",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                Spacer(modifier = Modifier.height(16.dp))
                SliderRow(
                    title = "亮度",
                    value = bgImageBrightness,
                    valueRange = 0f..100f
                ) { viewModel.setIntSetting(KEY_BG_IMAGE_BRIGHTNESS, it) }

                SliderRow(
                    title = "模糊",
                    value = bgImageBlur,
                    valueRange = 0f..50f
                ) { viewModel.setIntSetting(KEY_BG_IMAGE_BLUR, it) }
            }
        }

        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 160.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun TypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    swatchColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.toString(),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
