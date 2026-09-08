package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun observeSettingsScroll(
    listState: LazyListState,
    onScrolledChange: (Boolean) -> Unit
) {
    LaunchedEffect(listState, onScrolledChange) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }.collect(onScrolledChange)
    }
}
