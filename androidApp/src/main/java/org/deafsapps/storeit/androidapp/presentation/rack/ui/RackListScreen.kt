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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import org.deafsapps.storeit.androidapp.design.Dimens
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.rack.model.RackListUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RackListScreen(
    uiState: RackListUiState,
    uiEvent: () -> Flow<RackListUiEvent?>,
    onAddRackSelect: () -> Unit,
    onRackSelect: (Rack) -> Unit,
    onNavigateToAddRack: () -> Unit,
    onNavigateToRackDetail: (String) -> Unit,
    onNavigateToAddItem: () -> Unit = {},
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
                title = { Text("Racks") },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add item") },
                            onClick = {
                                showMenu = false
                                onNavigateToAddItem()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRackSelect,
                shape = RoundedCornerShape(Dimens.fabCornerRadius),
            ) {
                Text("+")
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                                onClick = { onRackSelect(rack) },
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
            text = "No racks yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Add your first storage rack to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onAddRackSelect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add Rack")
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
