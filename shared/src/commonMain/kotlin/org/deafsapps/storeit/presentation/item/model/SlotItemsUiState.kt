package org.deafsapps.storeit.presentation.item.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SlotItemsUiState(
    val items: ImmutableList<ItemSummaryVo>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): SlotItemsUiState = SlotItemsUiState(
            items = persistentListOf(),
            isLoading = true,
            error = null,
        )
    }
}

interface SlotItemsUiEvent
