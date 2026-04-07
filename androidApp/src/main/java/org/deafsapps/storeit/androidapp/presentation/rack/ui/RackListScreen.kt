package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import org.deafsapps.storeit.androidapp.design.Dimens
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.rack.model.RackListUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RackListScreen(
    uiState: RackListUiState,
    uiEvent: () -> Flow<RackListUiEvent?>,
    onAddRackSelect: () -> Unit,
    onRackSelected: (Rack) -> Unit,
    onNavigateToAddRack: () -> Unit,
    onNavigateToRackDetail: (String) -> Unit,
    onNavigateToAddItem: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    isDarkModeEnabled: Boolean = false,
    onThemeModeToggle: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            uiEvent().collect { event ->
                when (event) {
                    is RackListUiEvent.NavigateToAddRack -> onNavigateToAddRack()
                    is RackListUiEvent.NavigateToRackDetail -> onNavigateToRackDetail(event.rackId)
                    is RackListUiEvent.ShowError -> { }
                    null -> { }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rack_list_title)) },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("rackListOverflowMenuButton"),
                    ) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.testTag("rackListAddItemMenuItem"),
                            text = { Text(stringResource(R.string.rack_list_overflow_add_item)) },
                            onClick = {
                                showMenu = false
                                onNavigateToAddItem()
                            },
                        )
                        DropdownMenuItem(
                            modifier = Modifier.testTag("rackListThemeMenuItem"),
                            text = {
                                Text(
                                    stringResource(
                                        if (isDarkModeEnabled) {
                                            R.string.rack_list_overflow_light_mode
                                        } else {
                                            R.string.rack_list_overflow_dark_mode
                                        }
                                    )
                                )
                            },
                            onClick = {
                                showMenu = false
                                onThemeModeToggle()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRackSelect,
                modifier = Modifier.testTag("rackListAddRackFab"),
                shape = RoundedCornerShape(Dimens.fabCornerRadius),
            ) {
                Text("+")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.listContentPadding)
                    .padding(bottom = Dimens.listItemSpacing),
            ) {
                OutlinedTextField(
                    value = "",
                    readOnly = true,
                    enabled = false,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.rack_list_search_placeholder)) },
                    singleLine = true,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onNavigateToSearch)
                        .testTag("rackListSearchField"),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(Dimens.progressIndicatorSizeLarge),
                        )
                    }
                    uiState.racks.isEmpty() -> {
                        EmptyState(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(Dimens.screenPaddingLarge),
                            onAddRackSelect = onAddRackSelect,
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(Dimens.listContentPadding),
                            verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
                        ) {
                            items(uiState.racks, key = { it.id }) { rack ->
                                RackListItem(
                                    rack = rack,
                                    onClick = { onRackSelected(rack) },
                                )
                            }
                        }
                    }
                }
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(Dimens.screenPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddRackSelect: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.emptyStateVerticalSpacing),
    ) {
        Text(
            text = stringResource(R.string.rack_list_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.rack_list_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onAddRackSelect,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rackListEmptyStateAddRackButton"),
        ) {
            Text(stringResource(R.string.rack_list_empty_add_rack))
        }
    }
}

@Composable
private fun RackListItem(
    rack: Rack,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rackRowViewButton")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cardCornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            if (rack.photoUri != null) {
                AsyncImage(
                    model = rack.photoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall))
                        .height(Dimens.rackListItemImageHeight),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.size(Dimens.rackListItemContentSpacing))
            }
            Text(
                text = rack.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (rack.location.isNotBlank()) {
                Text(
                    text = rack.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RackListScreenPreview() {
    val sampleRacks = listOf(
        Rack(
            id = "1",
            name = "Garage shelf",
            description = "Main storage shelf in garage",
            location = "Garage",
            photoUri = null,
        ),
        Rack(
            id = "2",
            name = "Kitchen pantry",
            description = "Pantry in the kitchen",
            location = "Kitchen",
            photoUri = null,
        )
    )
    MaterialTheme {
        RackListScreen(
            uiState = RackListUiState(
                racks = sampleRacks,
                isLoading = false,
                error = null
            ),
            uiEvent = { emptyFlow() },
            onAddRackSelect = {},
            onRackSelected = {},
            onNavigateToAddRack = {},
            onNavigateToRackDetail = {},
            onNavigateToAddItem = {},
            onNavigateToSearch = {},
            isDarkModeEnabled = false,
            onThemeModeToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RackListScreenEmptyPreview() {
    MaterialTheme {
        RackListScreen(
            uiState = RackListUiState(
                racks = emptyList(),
                isLoading = false,
                error = null
            ),
            uiEvent = { emptyFlow() },
            onAddRackSelect = {},
            onRackSelected = {},
            onNavigateToAddRack = {},
            onNavigateToRackDetail = {},
            onNavigateToAddItem = {},
            onNavigateToSearch = {},
            isDarkModeEnabled = false,
            onThemeModeToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RackListScreenLoadingPreview() {
    MaterialTheme {
        RackListScreen(
            uiState = RackListUiState(
                racks = emptyList(),
                isLoading = true,
                error = null
            ),
            uiEvent = { emptyFlow() },
            onAddRackSelect = {},
            onRackSelected = {},
            onNavigateToAddRack = {},
            onNavigateToRackDetail = {},
            onNavigateToAddItem = {},
            onNavigateToSearch = {},
            isDarkModeEnabled = false,
            onThemeModeToggle = {},
        )
    }
}
