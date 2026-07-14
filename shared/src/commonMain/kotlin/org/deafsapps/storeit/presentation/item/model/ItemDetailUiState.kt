package org.deafsapps.storeit.presentation.item.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ItemDetailUiState(
    val isLoading: Boolean,
    val isSaving: Boolean,
    val error: String?,
    val name: String,
    val description: String,
    val quantity: Int?,
    val owner: String,
    val tags: ImmutableList<String>,
    val tagInput: String,
    val photoUri: String?,
    val showDeleteConfirm: Boolean,
) {
    companion object {
        fun getDefault(): ItemDetailUiState = ItemDetailUiState(
            isLoading = true,
            isSaving = false,
            error = null,
            name = "",
            description = "",
            quantity = null,
            owner = "",
            tags = persistentListOf(),
            tagInput = "",
            photoUri = null,
            showDeleteConfirm = false,
        )
    }
}

sealed interface ItemDetailUiEvent {
    data object NavigateBack : ItemDetailUiEvent
    data class ShowError(val message: String) : ItemDetailUiEvent
}
