package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.fhreborn.ui.viewmodel.SortField
import cn.lemondrop.fhreborn.ui.viewmodel.SortOrder
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text

@Composable
fun SortSheet(
    currentField: SortField,
    currentOrder: SortOrder,
    onDismiss: () -> Unit,
    onSelectField: (SortField) -> Unit,
    onToggleOrder: () -> Unit
) {
    val sortOptions = listOf(
        SortField.TITLE to "标题",
        SortField.ARTIST_ALBUM to "艺术家 - 专辑",
        SortField.ALBUM_DISC_TRACK to "专辑 - 碟号 - 音轨号",
        SortField.MODIFIED_TIME to "修改时间",
        SortField.ADDED_TIME to "添加时间",
        SortField.PLAY_COUNT to "播放次数",
        SortField.PATH_FILENAME to "路径 - 文件名",
        SortField.FILE_NAME to "文件名",
        SortField.RELEASE_YEAR to "发行时间",
        SortField.DURATION to "曲目时长"
    )

    CloverBottomSheet(
        onDismiss = onDismiss,
        title = "排序"
    ) {
        // 升序/降序切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onToggleOrder)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (currentOrder == SortOrder.ASC) Lucide.ArrowUp else Lucide.ArrowDown,
                    contentDescription = if (currentOrder == SortOrder.ASC) "升序" else "降序",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentOrder == SortOrder.ASC) "升序" else "降序",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // 排序选项
        LazyColumn {
            items(sortOptions) { (field, label) ->
                val isSelected = field == currentField
                CloverMenuItem(
                    label = label,
                    icon = if (isSelected) Lucide.Check else null,
                    onClick = {
                        onSelectField(field)
                        onDismiss()
                    }
                )
            }
        }
    }
}
