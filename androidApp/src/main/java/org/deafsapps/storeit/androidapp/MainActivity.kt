package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.App
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    val viewModel: AddRackViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AddRackScreen(
                viewModel = viewModel,
                onNavigateBack = {},
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
