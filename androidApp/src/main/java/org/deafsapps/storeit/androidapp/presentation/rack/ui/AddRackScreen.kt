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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackUiEvent
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRackScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddRackViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { uiEvent ->
                when (uiEvent) {
                    is AddRackUiEvent.NavigateBack -> {
                        onNavigateBack()
                    }
                    is AddRackUiEvent.ShowError -> {
                    }
                    null -> {
                        println("UI event not recognised!")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Rack") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoPickerSection(
                photoUri = uiState.photoUri,
                onPhotoSelected = { uri -> viewModel.updatePhotoUri(uri) },
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Button(
                onClick = { viewModel.saveRack() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save Rack")
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
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Rack photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("No photo selected")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showImagePicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (photoUri != null) "Change Photo" else "Select Photo")
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
