package org.deafsapps.storeit.androidapp.presentation.rack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.deafsapps.storeit.domain.usecase.DeleteRackUseCaseType
import org.deafsapps.storeit.domain.usecase.GetRackDataByRackIdUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveRackUseCaseType
import org.deafsapps.storeit.domain.usecase.SaveSlotUseCaseType
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class AndroidRackDetailViewModel(
    @InjectedParam private val rackId: String,
    private val getRackDataByRackIdUseCase: GetRackDataByRackIdUseCaseType,
    private val saveSlotUseCase: SaveSlotUseCaseType,
    private val saveRackUseCase: SaveRackUseCaseType,
    private val deleteRackUseCase: DeleteRackUseCaseType,
) : ViewModel() {

    val rackDetailViewModel: RackDetailViewModel =
        RackDetailViewModel(
            coroutineScope = viewModelScope,
            rackId = rackId,
            getRackDataByRackIdUseCase = getRackDataByRackIdUseCase,
            saveSlotUseCase = saveSlotUseCase,
            saveRackUseCase = saveRackUseCase,
            deleteRackUseCase = deleteRackUseCase,
        )

    override fun onCleared() {
        rackDetailViewModel.clear()
        super.onCleared()
    }
}
