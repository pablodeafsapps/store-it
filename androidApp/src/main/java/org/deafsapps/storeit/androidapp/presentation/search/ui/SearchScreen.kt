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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.domain.model.ItemWithPlacement
import org.deafsapps.storeit.presentation.search.model.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onItemSelected: (ItemWithPlacement) -> Unit,
    onNavigateBack: () -> Unit,
) {
    SearchScreenContent(
        uiState = uiState,
        onQueryChange = onQueryChange,
        onNavigateBack = onNavigateBack,
        onItemSelected = onItemSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onItemSelected: (ItemWithPlacement) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search items",
                        modifier = Modifier.testTag("searchScreenTitle"),
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("searchScreenBackButton"),
                    ) {
                        Text("Back")
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
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("searchScreenQueryField"),
                placeholder = { Text("Search by name or description") },
                singleLine = true,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("searchScreenLoading"),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(top = Dimens.screenPadding)
                                .testTag("searchScreenError"),
                        )
                    }
                    uiState.query.isBlank() -> {
                        Text(
                            text = "Type to search your items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = Dimens.screenPadding)
                                .testTag("searchScreenHint"),
                        )
                    }
                    uiState.results.isEmpty() -> {
                        Text(
                            text = "No items match your search.",
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
                            items(uiState.results, key = { it.item.id }) { row ->
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
    row: ItemWithPlacement,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.listItemSpacing / 2)
            .testTag("searchResultRow_${row.item.id}")
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text(
                text = row.item.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("searchResultItemName_${row.item.id}"),
            )
            Text(
                text = "Rack: ${row.rackName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Slot: ${row.slotSummary}",
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
            uiState = SearchUiState(
                query = "tool",
                results = emptyList(),
                isLoading = false,
                error = null,
            ),
            onQueryChange = {},
            onNavigateBack = {},
            onItemSelected = {},
        )
    }
}
