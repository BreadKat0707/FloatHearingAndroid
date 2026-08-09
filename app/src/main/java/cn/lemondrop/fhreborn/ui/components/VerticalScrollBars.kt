@file:OptIn(top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi::class)

package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter

/**
 * miuix 竖向滚动条封装（自动淡入淡出，可拖动定位）。
 * 用法：列表外层 Box 中 `LazyListScrollBar(listState, Modifier.align(Alignment.CenterEnd))`。
 */
@Composable
fun LazyListScrollBar(listState: LazyListState, modifier: Modifier = Modifier) {
    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(listState),
        modifier = modifier
    )
}

/** 网格列表（LazyVerticalGrid）滚动条封装 */
@Composable
fun LazyGridScrollBar(gridState: LazyGridState, modifier: Modifier = Modifier) {
    VerticalScrollBar(
        adapter = rememberScrollBarAdapter(gridState),
        modifier = modifier
    )
}
