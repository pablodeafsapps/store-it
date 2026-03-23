package org.deafsapps.storeit.presentation.search.model

import androidx.compose.runtime.Immutable
import org.deafsapps.storeit.domain.model.ItemWithPlacement

@Immutable
data class SearchUiState(
    val query: String,
    val results: List<ItemWithPlacement>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): SearchUiState = SearchUiState(
            query = "",
            results = emptyList(),
            isLoading = false,
            error = null,
        )
    }
}
