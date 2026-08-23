package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 列表布局选择弹窗：单选底部弹窗（radio 式，选中对号 + primary 色）。
 *
 * miuix 0.9.4 的 DropdownItem.children 只定义了字段、渲染端（OverlayDropdownPopup）
 * 未实现级联子菜单，因此布局选择用独立弹窗呈现。
 *
 * @param title 弹窗标题
 * @param options (样式 key, 显示文案) 列表
 * @param currentStyle 当前选中 key
 * @param onSelect 选中回调
 * @param onDismiss 关闭回调
 */
@Composable
fun LayoutStyleSheet(
    title: String,
    options: List<Pair<String, String>>,
    currentStyle: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = title,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn {
            items(options, key = { it.first }) { (key, label) ->
                val isSelected = key == currentStyle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(key)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 固定前导宽度：选中显示对号，未选中留空占位，文字始终对齐
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "已选择",
                                modifier = Modifier.size(18.dp),
                                tint = MiuixTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body1,
                        color = if (isSelected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
