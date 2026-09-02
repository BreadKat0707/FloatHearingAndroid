package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.ui.screens.player.PlayerBackgroundType
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 播放器页面背景选择内容（纯内容组件，不带外壳）。
 * 由 SettingsScreen 统一外壳管理并内嵌显示。
 */
@Composable
fun PlayerBackgroundPickerContent(
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val initialBgKey = remember { kotlinx.coroutines.runBlocking { repository.getString("player_bg", PlayerBackgroundType.CoverBlur.key).first() } }
    val currentKey by repository.getString("player_bg", PlayerBackgroundType.CoverBlur.key)
        .collectAsState(initial = initialBgKey)

    val options = listOf(
        PlayerBackgroundType.AppleMusic to "Apple Music 流体背景",
        PlayerBackgroundType.AgslFluid to "AGSL 流体背景",
        PlayerBackgroundType.CoverBlur to "专辑封面模糊",
        PlayerBackgroundType.DefaultColor to "默认背景色"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = bottomOverlayHeight + 16.dp
        )
    ) {
        item {
            Text(
                text = "选择播放器页面的背景样式",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        options.forEach { (type, label) ->
            item(key = type.key) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    repository.setString("player_bg", type.key)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentKey == type.key,
                            onClick = {
                                scope.launch {
                                    repository.setString("player_bg", type.key)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label)
                    }
                    HorizontalDivider()
                }
            }
        }

        // Apple Music 背景设置面板
        if (currentKey == PlayerBackgroundType.AppleMusic.key) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { AppleMusicSettingsPanel(repository, scope) }
        }
    }
}

@Composable
private fun AppleMusicSettingsPanel(
    repository: AppSettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val initialBlurDp = remember { kotlinx.coroutines.runBlocking { repository.appleMusicBlurDp.first() } }
    val initialScrimPct = remember { kotlinx.coroutines.runBlocking { repository.appleMusicScrimPct.first() } }
    val initialSpeed = remember { kotlinx.coroutines.runBlocking { repository.appleMusicSpeed.first() } }
    val initialCrossfadeMs = remember { kotlinx.coroutines.runBlocking { repository.appleMusicCrossfadeMs.first() } }
    val initialSaturation = remember { kotlinx.coroutines.runBlocking { repository.appleMusicSaturation.first() } }
    val initialRenderScale = remember { kotlinx.coroutines.runBlocking { repository.appleMusicRenderScale.first() } }
    val initialBassPulse = remember { kotlinx.coroutines.runBlocking { repository.appleMusicBassPulse.first() } }

    val blurDp by repository.appleMusicBlurDp.collectAsState(initial = initialBlurDp)
    val scrimPct by repository.appleMusicScrimPct.collectAsState(initial = initialScrimPct)
    val speed by repository.appleMusicSpeed.collectAsState(initial = initialSpeed)
    val crossfadeMs by repository.appleMusicCrossfadeMs.collectAsState(initial = initialCrossfadeMs)
    val saturation by repository.appleMusicSaturation.collectAsState(initial = initialSaturation)
    val renderScale by repository.appleMusicRenderScale.collectAsState(initial = initialRenderScale)
    val bassPulse by repository.appleMusicBassPulse.collectAsState(initial = initialBassPulse)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 模糊强度
        SettingSliderItem(
            label = "模糊强度",
            value = blurDp.toFloat(),
            valueRange = 0f..100f,
            steps = 99,
            displayText = "${blurDp}dp",
            onValueChange = { scope.launch { repository.setAppleMusicBlurDp(it.toInt()) } }
        )

        // 暗色遮罩
        SettingSliderItem(
            label = "暗色遮罩",
            value = scrimPct.toFloat(),
            valueRange = 0f..100f,
            steps = 99,
            displayText = "${scrimPct}%",
            onValueChange = { scope.launch { repository.setAppleMusicScrimPct(it.toInt()) } }
        )

        // 旋转速度
        SettingSliderItem(
            label = "旋转速度",
            value = speed.toFloat(),
            valueRange = 0.25f..4f,
            steps = 0,
            displayText = String.format("%.2fx", speed),
            onValueChange = { scope.launch { repository.setAppleMusicSpeed(it.toDouble()) } }
        )

        // 过渡时间
        SettingSliderItem(
            label = "切歌过渡时间",
            value = crossfadeMs.toFloat(),
            valueRange = 0f..1200f,
            steps = 0,
            displayText = "${crossfadeMs}ms",
            onValueChange = { scope.launch { repository.setAppleMusicCrossfadeMs(it.toInt()) } }
        )

        // 色彩饱和度
        SettingSliderItem(
            label = "色彩饱和度",
            value = saturation.toFloat(),
            valueRange = 0f..2f,
            steps = 0,
            displayText = String.format("%.0f%%", saturation * 100),
            onValueChange = { scope.launch { repository.setAppleMusicSaturation(it.toDouble()) } }
        )

        // 渲染分辨率
        SettingSliderItem(
            label = "渲染分辨率",
            value = renderScale.toFloat(),
            valueRange = 0.25f..1f,
            steps = 0,
            displayText = String.format("%.0f%%", renderScale * 100),
            onValueChange = { scope.launch { repository.setAppleMusicRenderScale(it.toDouble()) } }
        )

        // 低音脉冲开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "低音脉冲",
                    style = MiuixTheme.textStyles.body1
                )
                Text(
                    text = "随音乐节拍律动",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            Switch(
                checked = bassPulse,
                onCheckedChange = { scope.launch { repository.setAppleMusicBassPulse(it) } }
            )
        }
    }
}

@Composable
private fun SettingSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun CoverAppearanceSettingsPanel(
    repository: AppSettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val initialShadowY = remember { kotlinx.coroutines.runBlocking { repository.playerCoverShadowY.first() } }
    val initialShadowAlpha = remember { kotlinx.coroutines.runBlocking { repository.playerCoverShadowAlpha.first() } }
    val initialShadowBlur = remember { kotlinx.coroutines.runBlocking { repository.playerCoverShadowBlur.first() } }
    val initialPauseScale = remember { kotlinx.coroutines.runBlocking { repository.playerCoverPauseScale.first() } }

    val shadowY by repository.playerCoverShadowY.collectAsState(initial = initialShadowY)
    val shadowAlpha by repository.playerCoverShadowAlpha.collectAsState(initial = initialShadowAlpha)
    val shadowBlur by repository.playerCoverShadowBlur.collectAsState(initial = initialShadowBlur)
    val pauseScale by repository.playerCoverPauseScale.collectAsState(initial = initialPauseScale)

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingSliderItem(
            label = "投影 Y 轴偏移",
            value = shadowY.toFloat(),
            valueRange = 0f..60f,
            steps = 59,
            displayText = "${shadowY}dp",
            onValueChange = { scope.launch { repository.setPlayerCoverShadowY(it.toInt()) } }
        )

        SettingSliderItem(
            label = "投影颜色浓度",
            value = shadowAlpha.toFloat(),
            valueRange = 0f..100f,
            steps = 99,
            displayText = "${shadowAlpha}%",
            onValueChange = { scope.launch { repository.setPlayerCoverShadowAlpha(it.toInt()) } }
        )

        SettingSliderItem(
            label = "投影模糊程度",
            value = shadowBlur.toFloat(),
            valueRange = 0f..60f,
            steps = 59,
            displayText = "${shadowBlur}dp",
            onValueChange = { scope.launch { repository.setPlayerCoverShadowBlur(it.toInt()) } }
        )

        SettingSliderItem(
            label = "暂停时缩小比例",
            value = pauseScale.toFloat(),
            valueRange = 50f..100f,
            steps = 49,
            displayText = "${pauseScale}%",
            onValueChange = { scope.launch { repository.setPlayerCoverPauseScale(it.toInt()) } }
        )
    }
}
