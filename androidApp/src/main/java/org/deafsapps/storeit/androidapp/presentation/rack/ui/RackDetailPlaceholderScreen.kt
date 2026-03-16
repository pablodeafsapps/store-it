package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.androidapp.design.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RackDetailPlaceholderScreen(
    rackId: String,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rack detail") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Rack: $rackId (detail screen coming in T014)")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RackDetailPlaceholderScreenPreview() {
    MaterialTheme {
        RackDetailPlaceholderScreen(
            rackId = "rack_123",
            onNavigateBack = {},
        )
    }
}
