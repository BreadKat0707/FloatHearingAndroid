package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.theme.isRuntimeShaderSupported
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.ListMinus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blendColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ToolbarBlurRadius = 40f
private const val ToolbarSurfaceAlpha = 0.8f

/**
 * 多选模式底部工具栏（导航栏样式）。
 * 结构同 NavigationBar：顶部描边 + 图标/文字标签工具项 + 底部系统导航栏避让。
 *
 * - 不再显示"已选 N"（标题栏已显示）
 * - [backdrop] 非空且平台支持时，工具栏透明并对页面内容做真实模糊（磨砂玻璃）；
 *   否则回退 surfaceContainer 色值
 * - [showSongActions] 为 false 时仅显示删除（退出多选在标题栏，工具栏不重复提供）
 */
@Composable
fun MultiSelectToolbar(
    selectedCount: Int,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    showSongActions: Boolean = true,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val enabled = selectedCount > 0
    val useBlur = backdrop != null && isRuntimeShaderSupported
    val surfaceColor = MiuixTheme.colorScheme.surface
    val effectColors = remember(surfaceColor) {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = surfaceColor.copy(alpha = ToolbarSurfaceAlpha))
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (useBlur) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop!!,
                        shape = { RoundedCornerShape(0.dp) },
                        effects = {
                            blur(radiusX = ToolbarBlurRadius)
                            blendColors(effectColors)
                        }
                    )
                } else {
                    Modifier.background(MiuixTheme.colorScheme.surfaceContainer)
                }
            )
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSongActions) {
                MultiSelectToolItem(
                    icon = Lucide.FolderPlus,
                    label = "加入",
                    enabled = enabled,
                    onClick = onAddToPlaylist
                )
                MultiSelectToolItem(
                    icon = Lucide.ListMusic,
                    label = "队列",
                    enabled = enabled,
                    onClick = onAddToQueue
                )
                MultiSelectToolItem(
                    icon = Lucide.Share2,
                    label = "分享",
                    enabled = enabled,
                    onClick = onShare
                )
            }
            if (onRemoveFromPlaylist != null) {
                MultiSelectToolItem(
                    icon = Lucide.ListMinus,
                    label = "移除",
                    enabled = enabled,
                    destructive = true,
                    onClick = onRemoveFromPlaylist
                )
            }
            MultiSelectToolItem(
                icon = Lucide.Trash2,
                label = "删除",
                enabled = enabled,
                destructive = true,
                onClick = onDelete
            )
        }
        // 底部系统导航栏避让（手势条/三键）
        Spacer(
            modifier = Modifier.height(
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        )
    }
}

@Composable
private fun MultiSelectToolItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val tint = when {
        !enabled -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = tint
        )
    }
}
