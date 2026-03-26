package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import org.deafsapps.storeit.androidapp.design.backArrowIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.androidapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RackDetailPlaceholderScreen(
    rackId: String,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rack_detail_placeholder_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.rack_detail_placeholder_body, rackId))
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
