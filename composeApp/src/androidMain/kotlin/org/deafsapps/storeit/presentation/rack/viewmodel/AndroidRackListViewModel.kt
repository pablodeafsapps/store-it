package org.deafsapps.storeit.presentation.rack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.deafsapps.storeit.domain.usecase.GetRacksUseCaseType
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AndroidRackListViewModel(
    private val getRacksUseCase: GetRacksUseCaseType,
) : ViewModel() {

    val rackListViewModel: RackListViewModel =
        RackListViewModel(coroutineScope = viewModelScope, getRacksUseCase = getRacksUseCase)

    override fun onCleared() {
        rackListViewModel.clear()
        super.onCleared()
    }
}
