package org.deafsapps.storeit.androidapp.presentation.rack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AndroidAddRackViewModel(
    private val saveRackUseCase: SaveRackUseCaseType,
) : ViewModel() {

    val addRackViewModel: AddRackViewModel =
        AddRackViewModel(coroutineScope = viewModelScope, saveRackUseCase = saveRackUseCase)

    override fun onCleared() {
        addRackViewModel.clear()
        super.onCleared()
    }
}
