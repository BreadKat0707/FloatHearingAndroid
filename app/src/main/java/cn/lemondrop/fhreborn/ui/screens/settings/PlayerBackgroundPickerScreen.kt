package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    val currentKey by repository.getString("player_bg", PlayerBackgroundType.CoverBlur.key)
        .collectAsState(initial = PlayerBackgroundType.CoverBlur.key)

    val options = listOf(
        PlayerBackgroundType.RotatingFluid to "旋转流体背景",
        PlayerBackgroundType.AgslFluid to "AGSL 流体背景",
        PlayerBackgroundType.AccordBlend to "Accord 流体背景",
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
    }
}