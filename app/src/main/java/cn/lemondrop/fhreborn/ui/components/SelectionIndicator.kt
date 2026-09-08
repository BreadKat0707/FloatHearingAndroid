package cn.lemondrop.fhreborn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox

/**
 * Selection marker used by song/playlist multi-select rows.
 * Backed by Miuix [Checkbox] so checked/unchecked transitions are animated.
 */
@Composable
fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Checkbox(
        state = if (selected) ToggleableState.On else ToggleableState.Off,
        onClick = null,
        modifier = modifier
    )
}
