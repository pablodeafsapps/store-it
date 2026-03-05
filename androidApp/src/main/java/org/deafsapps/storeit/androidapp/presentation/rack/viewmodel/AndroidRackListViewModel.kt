package org.deafsapps.storeit.androidapp.presentation.rack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.deafsapps.storeit.domain.usecase.GetRacksFlowUseCaseType
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
internal class AndroidRackListViewModel(
    private val getRacksUseCase: GetRacksFlowUseCaseType,
) : ViewModel() {

    val rackListViewModel: RackListViewModel =
        RackListViewModel(coroutineScope = viewModelScope, getRacksFlowUseCase = getRacksUseCase)

    override fun onCleared() {
        rackListViewModel.clear()
        super.onCleared()
    }
}
