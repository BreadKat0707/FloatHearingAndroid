package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 信息行：小字标签 + 正文值（上下结构），用于歌词信息/歌曲属性等只读弹窗。
 *
 * @param selectable 为 true 时值文本支持长按弹出系统文本选择（复制/全选）
 */
@Composable
fun InfoRow(label: String, value: String, selectable: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        val valueText: @Composable () -> Unit = {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (selectable) {
            SelectionContainer(content = valueText)
        } else {
            valueText()
        }
    }
}
