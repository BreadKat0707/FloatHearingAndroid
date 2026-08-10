package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用列表行：Miuix BasicComponent 布局 + 自绘文本。
 *
 * 与直接使用 BasicComponent 的区别：
 * - 标题用 body1（16sp）而非默认的 headline1（18sp）
 * - 标题/副标题均单行截断（BasicComponent 内置 Text 不支持 maxLines/overflow）
 *
 * @param title 标题
 * @param summary 副标题（可选）
 * @param leading 左侧内容（图标/封面等）
 * @param trailing 右侧内容（控件/箭头等）
 * @param onClick 点击回调，null 时不可点击
 */
@Composable
fun FhListItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
) {
    BasicComponent(
        modifier = modifier,
        startAction = leading,
        endActions = trailing,
        onClick = onClick,
        insideMargin = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
