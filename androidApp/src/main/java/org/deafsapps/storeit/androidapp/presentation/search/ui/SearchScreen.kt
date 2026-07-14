package org.deafsapps.storeit.androidapp.presentation.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import org.deafsapps.storeit.androidapp.design.backArrowIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.presentation.search.model.SearchResultVo
import org.deafsapps.storeit.presentation.search.model.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onItemSelected: (SearchResultVo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    SearchScreenContent(
        query = uiState.query,
        results = uiState.results,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onQueryChange = onQueryChange,
        onNavigateBack = onNavigateBack,
        onItemSelected = onItemSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    query: String,
    results: List<SearchResultVo>,
    isLoading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onItemSelected: (SearchResultVo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.search_title),
                        modifier = Modifier.testTag("searchScreenTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("searchScreenBackButton"),
                    ) {
                        Icon(
                            imageVector = backArrowIcon,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimens.screenPadding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("searchScreenQueryField"),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("searchScreenLoading"),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(top = Dimens.screenPadding)
                                .testTag("searchScreenError"),
                        )
                    }
                    query.isBlank() -> {
                        Text(
                            text = stringResource(R.string.search_hint_type_to_search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = Dimens.screenPadding)
                                .testTag("searchScreenHint"),
                        )
                    }
                    results.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = Dimens.screenPadding)
                                .testTag("searchScreenNoResults"),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = Dimens.listContentPadding)
                                .testTag("searchScreenResults"),
                        ) {
                            items(results, key = { it.itemId }) { row ->
                                SearchResultRow(
                                    row = row,
                                    onClick = { onItemSelected(row) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    row: SearchResultVo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.listItemSpacing / 2)
            .testTag("searchResultRow_${row.itemId}")
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text(
                text = row.itemName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("searchResultItemName_${row.itemId}"),
            )
            Text(
                text = stringResource(R.string.search_result_rack_prefix, row.rackName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.search_result_slot_prefix, row.slotSummary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenContentPreview() {
    MaterialTheme {
        SearchScreenContent(
            query = "tool",
            results = persistentListOf(),
            isLoading = false,
            error = null,
            onQueryChange = {},
            onNavigateBack = {},
            onItemSelected = {},
        )
    }
}
