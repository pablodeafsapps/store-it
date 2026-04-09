package org.deafsapps.storeit.presentation.item.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.Item

@Immutable
data class SlotItemsUiState(
    val items: List<Item>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): SlotItemsUiState = SlotItemsUiState(
            items = emptyList(),
            isLoading = true,
            error = null,
        )
    }
}

interface SlotItemsUiEvent
