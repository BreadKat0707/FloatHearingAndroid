package cn.lemondrop.fhreborn.ui.screens.settings

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.clover.CloverDialog
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.fhreborn.data.model.SettingItem
import cn.lemondrop.fhreborn.data.model.SettingType
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.components.MainScaffold
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.SettingsViewModel
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Slider
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import kotlinx.coroutines.launch

/**
 * Accompanist Lyric 设置页。
 * 提供歌词文字大小、粗细、翻译/音标显示、非当前行模糊、对齐方式等配置。
 */
@Composable
fun AccompanistLyricSettingsScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
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

    BackHandler { onBack() }

    MainScaffold(
        playerViewModel = playerViewModel,
        currentRoute = "accompanist_lyric_settings",
        onNavigate = { },
        title = { Text("Accompanist Lyric 设置") },
        onPlayerClick = { }
    ) { paddingValues, bottomOverlayHeight, _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = CloverSizes.listOuterHorizontalPadding,
                end = CloverSizes.listOuterHorizontalPadding,
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

            // 模糊
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

            // 对齐方式
            item { SectionHeader("对齐方式") }
            item {
                SelectionSettingItem(
                    title = "歌词对齐",
                    selectedLabel = when (textAlign) {
                        "start" -> "居左"
                        "end" -> "居右"
                        else -> "居中"
                    },
                    options = listOf("start" to "居左", "center" to "居中", "end" to "居右"),
                    onSelected = { scope.launch { repository.setAcclLyricTextAlign(it) } }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    enabled: Boolean = true,
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
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    cn.lemondrop.fhreborn.ui.components.FhListItem(
        title = title,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switcher(
                checked = checked,
                onCheckStateChange = onCheckedChange
            )
        }
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
        CloverDialog(
            onDismissRequest = { showDialog = false },
            title = title,
            buttons = { }
        ) {
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
    }

    cn.lemondrop.fhreborn.ui.components.FhListItem(
        title = title,
        onClick = { showDialog = true },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = selectedLabel)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
