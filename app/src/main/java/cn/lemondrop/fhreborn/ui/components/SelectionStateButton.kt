package cn.lemondrop.fhreborn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.IconButton

/**
 * Header select-all control for multi-select mode.
 * Uses Miuix [Checkbox] tri-state so the checkbox animates between
 * Off, Indeterminate and On states.
 */
@Composable
fun SelectionStateButton(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = when {
        totalCount <= 0 || selectedCount <= 0 -> ToggleableState.Off
        selectedCount >= totalCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    IconButton(
        onClick = {
            if (state == ToggleableState.On) onDeselectAll() else onSelectAll()
        },
        modifier = modifier
    ) {
        Checkbox(
            state = state,
            onClick = null
        )
    }
}
