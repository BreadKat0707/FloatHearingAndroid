@file:OptIn(top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi::class)

package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 竖向滚动条封装（**常显**：基础淡色常驻，悬停/拖动加亮加宽，可拖动定位）。
 *
 * 基于 miuix ScrollBar 源码 vendor（Apache-2.0，见 MiuixScrollBar.kt）并修改：
 * 1. 修复轨道短于最小滑块时 coerceIn 崩溃；
 * 2. 改为常显（原版滚动后 1 秒淡出隐藏）。
 *
 * 用法：列表外层 Box 中 `LazyListScrollBar(listState, Modifier.align(Alignment.CenterEnd))`。
 *
 * @param trackPadding 轨道上下留白（跳过标题栏/导航栏/底部占位区域）
 */
@Composable
fun LazyListScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    trackPadding: PaddingValues = PaddingValues(0.dp)
) {
    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(listState),
        modifier = modifier.padding(trackPadding)
    )
}

/** 网格列表（LazyVerticalGrid）滚动条封装 */
@Composable
fun LazyGridScrollBar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    trackPadding: PaddingValues = PaddingValues(0.dp)
) {
    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(gridState),
        modifier = modifier.padding(trackPadding)
    )
}
