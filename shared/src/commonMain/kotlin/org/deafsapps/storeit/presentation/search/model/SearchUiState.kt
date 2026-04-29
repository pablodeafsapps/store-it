package org.deafsapps.storeit.presentation.search.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SearchUiState(
    val query: String,
    val results: ImmutableList<SearchResultVo>,
    val isLoading: Boolean,
    val error: String?,
) {
    companion object {
        fun getDefault(): SearchUiState = SearchUiState(
            query = "",
            results = persistentListOf(),
            isLoading = false,
            error = null,
        )
    }
}
