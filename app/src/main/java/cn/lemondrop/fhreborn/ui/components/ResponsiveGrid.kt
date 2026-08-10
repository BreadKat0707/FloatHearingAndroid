package cn.lemondrop.fhreborn.ui.components

import androidx.compose.ui.unit.Dp

/**
 * 按内容区实际宽度计算响应式网格列数：
 * 列数 = 内容宽度 / 目标最小卡片宽，至少 [minColumns] 列，防御性上限 [maxColumns]。
 *
 * 与 `BoxWithConstraints` 搭配使用（[BoxWithConstraints.maxWidth] 即内容区宽度，
 * 侧边栏收折、窗口拉伸时自动重组）：宽度越大列数越多，卡片宽度恒不低于
 * [minItemWidthDp]，不会出现"卡片过大浪费空间"或"卡片过小"的问题。
 */
fun responsiveColumnCount(
    contentWidth: Dp,
    minItemWidthDp: Int,
    minColumns: Int,
    maxColumns: Int = 20
): Int {
    return (contentWidth.value / minItemWidthDp).toInt().coerceIn(minColumns, maxColumns)
}
