package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.viewmodel.SortField
import cn.lemondrop.fhreborn.ui.viewmodel.SortOrder
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SortSheet(
    options: List<Pair<SortField, String>>,
    currentField: SortField,
    currentOrder: SortOrder,
    onDismiss: () -> Unit,
    onSelectField: (SortField) -> Unit,
    onToggleOrder: () -> Unit
) {
    val sortOptions = options

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "排序",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        // 升序/降序：两个按钮，选中的用 primary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrderButton(
                label = "升序",
                icon = Lucide.ArrowUp,
                selected = currentOrder == SortOrder.ASC,
                onClick = { if (currentOrder != SortOrder.ASC) onToggleOrder() },
                modifier = Modifier.weight(1f)
            )
            SortOrderButton(
                label = "降序",
                icon = Lucide.ArrowDown,
                selected = currentOrder == SortOrder.DESC,
                onClick = { if (currentOrder != SortOrder.DESC) onToggleOrder() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        // 排序选项：radio 式，固定前导宽度保证文字对齐
        LazyColumn {
            items(sortOptions) { (field, label) ->
                val isSelected = field == currentField
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectField(field)
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

/** 升/降序切换按钮：选中时 primary 填充 */
@Composable
private fun SortOrderButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) MiuixTheme.colorScheme.onPrimary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = if (selected) MiuixTheme.colorScheme.onPrimary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
