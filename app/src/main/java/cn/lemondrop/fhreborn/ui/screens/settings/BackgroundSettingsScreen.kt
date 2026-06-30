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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
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
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import kotlinx.coroutines.launch

private const val KEY_BG_TYPE = "bg_type"
private const val KEY_BG_COLOR = "bg_color"
private const val KEY_BG_IMAGE_PATH = "bg_image_path"
private const val KEY_BG_IMAGE_BRIGHTNESS = "bg_image_brightness"
private const val KEY_BG_IMAGE_BLUR = "bg_image_blur"
private const val KEY_BG_MICA_BLUR = "bg_mica_blur"
private const val KEY_BG_MICA_ALT = "bg_mica_alt"

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
 * 主页面背景设置子页内容（作为 SettingsScreen 的 content 子视图使用，不自带 MainScaffold）。
 *
 * 三选一：纯色 / 自选图片（亮度 + 模糊）/ 云母（系统壁纸实时透出 + 模糊 + tint/噪点）。
 */
@Composable
fun BackgroundSettingsContent(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgType by viewModel.getStringValue(KEY_BG_TYPE, "color").collectAsState(initial = "color")
    val bgColor by viewModel.getStringValue(KEY_BG_COLOR, "").collectAsState(initial = "")
    val bgImagePath by viewModel.getStringValue(KEY_BG_IMAGE_PATH, "").collectAsState(initial = "")
    val bgImageBrightness by viewModel.getIntValue(KEY_BG_IMAGE_BRIGHTNESS, 100).collectAsState(initial = 100)
    val bgImageBlur by viewModel.getIntValue(KEY_BG_IMAGE_BLUR, 0).collectAsState(initial = 0)
    val bgMicaBlur by viewModel.getIntValue(KEY_BG_MICA_BLUR, 80).collectAsState(initial = 80)
    val bgMicaAlt by viewModel.repository.getBoolean(KEY_BG_MICA_ALT, false).collectAsState(initial = false)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        SectionTitle("背景类型")

        TypeOption("纯色", selected = bgType == "color") {
            viewModel.setStringSetting(KEY_BG_TYPE, "color")
        }
        TypeOption("自选图片", selected = bgType == "image") {
            viewModel.setStringSetting(KEY_BG_TYPE, "image")
        }
        TypeOption("云母（系统壁纸 · 实验性）", selected = bgType == "mica") {
            viewModel.setStringSetting(KEY_BG_TYPE, "mica")
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
                                MaterialTheme.colorScheme.background
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (previewBitmap != null) "点击预览可重新选择图片" else "尚未选择图片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            "mica" -> {
                SectionTitle("云母")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠ 实验性功能：目前仅实现壁纸模糊，材质显示效果尚未调整，会有显示异常（拖影、内容叠叠），极度不建议开启。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                SliderRow(
                    title = "模糊半径",
                    value = bgMicaBlur,
                    valueRange = 0f..150f
                ) { viewModel.setIntSetting(KEY_BG_MICA_BLUR, it) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "替代材质",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "切换云母的 tint/噪点风格",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switcher(
                        checked = bgMicaAlt,
                        onCheckStateChange = { checked ->
                            scope.launch { viewModel.repository.setBoolean(KEY_BG_MICA_ALT, checked) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "云母会实时透出系统壁纸并跨窗口模糊，需设备开启跨窗口模糊（受省电模式/开发者选项/GPU 影响）；动态壁纸会跟随变化。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
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
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
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
