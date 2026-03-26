package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import org.deafsapps.storeit.androidapp.design.Dimens
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.closeIcon
import org.deafsapps.storeit.presentation.rack.model.AddRackUiEvent
import org.deafsapps.storeit.presentation.rack.model.AddRackUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRackScreen(
    uiState: AddRackUiState,
    uiEvent: () -> Flow<AddRackUiEvent?>,
    onUpdatePhotoUri: (String) -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateLocation: (String) -> Unit,
    onSaveRack: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            uiEvent().collect { event ->
                when (event) {
                    is AddRackUiEvent.NavigateBack -> onNavigateBack()
                    is AddRackUiEvent.ShowError -> { }
                    null -> println("UI event not recognised!")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_rack_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("addRackCancelButton"),
                    ) {
                        Icon(
                            imageVector = closeIcon,
                            contentDescription = stringResource(R.string.common_cancel),
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
                .padding(Dimens.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingDefault),
        ) {
            PhotoPickerSection(
                photoUri = uiState.photoUri,
                onPhotoSelected = { uri -> onUpdatePhotoUri(uri) },
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { name -> onUpdateName(name) },
                label = { Text(stringResource(R.string.rack_name_required_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addRackNameField"),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { descr -> onUpdateDescription(descr) },
                label = { Text(stringResource(R.string.rack_description_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addRackDescriptionField"),
                maxLines = 3,
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = { loc -> onUpdateLocation(loc) },
                label = { Text(stringResource(R.string.rack_location_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("addRackLocationField"),
                singleLine = true,
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                )
            }

            Button(
                onClick = onSaveRack,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saveRackButton"),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(Dimens.progressIndicatorSizeSmall)
                            .testTag("saveRackProgress"),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.rack_save_button))
                }
            }
        }
    }
}

@Composable
private fun PhotoPickerSection(
    photoUri: String?,
    onPhotoSelected: (String) -> Unit,
) {
    var showImagePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.photoPickerCardHeight)
                .clip(RoundedCornerShape(Dimens.cardCornerRadiusSmall)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = stringResource(R.string.rack_photo_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(stringResource(R.string.photo_none_selected))
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.photoPickerSectionSpacing))

        Button(
            onClick = { showImagePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("addRackPhotoPickerButton"),
        ) {
            Text(
                if (photoUri != null) stringResource(R.string.photo_change)
                else stringResource(R.string.photo_select),
            )
        }
    }

    if (showImagePicker) {
        ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            onImageSelected = { uri ->
                onPhotoSelected(uri)
                showImagePicker = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddRackScreenPreview() {
    MaterialTheme {
        AddRackScreen(
            uiState = AddRackUiState(
                name = "Main Garage Rack",
                description = "Primary storage for tools and equipment",
                location = "Garage - East Wall",
                photoUri = null,
                isLoading = false,
                error = null,
                isSuccess = false,
            ),
            uiEvent = { flowOf(null) },
            onUpdatePhotoUri = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateLocation = {},
            onSaveRack = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddRackScreenLoadingPreview() {
    MaterialTheme {
        AddRackScreen(
            uiState = AddRackUiState.getDefault().copy(
                isLoading = true
            ),
            uiEvent = { flowOf(null) },
            onUpdatePhotoUri = {},
            onUpdateName = {},
            onUpdateDescription = {},
            onUpdateLocation = {},
            onSaveRack = {},
            onNavigateBack = {},
        )
    }
}
