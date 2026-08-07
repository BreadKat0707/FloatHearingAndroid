package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 项目统一的 BottomSheet 包装：
 * - 去掉 Miuix 默认的左右间距（内容自带 padding）
 * - 标题为居中的三级标题（title3）
 * - 底部留出系统导航栏区域，避免最后一项被手势条/导航键遮挡
 */
@Composable
fun FhBottomSheet(
    show: Boolean,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backgroundColor = backgroundColor,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.headline2,
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                content()
                // 底部留出系统导航栏区域，最后一项可正常点击
                val navBarBottom = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()
                Spacer(modifier = Modifier.height(navBarBottom))
            }
        }
    )
}
