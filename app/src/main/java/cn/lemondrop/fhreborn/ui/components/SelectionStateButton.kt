package cn.lemondrop.fhreborn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.SquareCheck
import com.composables.icons.lucide.SquareMinus
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 多选标题栏的"选择状态"按钮，三态：
 * - 全未选：空选框 [Square]，点击全选；
 * - 选中多个：[SquareMinus]，点击全选；
 * - 全选：[SquareCheck]（primary 高亮），点击取消全选。
 */
@Composable
fun SelectionStateButton(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = totalCount > 0 && selectedCount >= totalCount
    val icon: ImageVector = when {
        allSelected -> Lucide.SquareCheck
        selectedCount > 0 -> Lucide.SquareMinus
        else -> Lucide.Square
    }
    IconButton(
        onClick = { if (allSelected) onDeselectAll() else onSelectAll() },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (allSelected) "取消全选" else "全选",
            tint = if (allSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
        )
    }
}
