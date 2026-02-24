package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.App
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.data.repository.InMemoryRackRepository
import org.deafsapps.storeit.domain.usecase.SaveRackUseCase
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AddRackScreen(
                onNavigateBack = {},
                viewModel = AddRackViewModel(
                    saveRackUseCase = SaveRackUseCase(rackRepository = InMemoryRackRepository()),
                ),
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
