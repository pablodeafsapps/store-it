package org.deafsapps.storeit.di

import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val rackDetailModule: Module = module {
    viewModel { (rackId: String) ->
        RackDetailViewModel(
            rackId = rackId,
            getRackByIdUseCase = get(),
            getSlotsByRackUseCase = get(),
            saveSlotUseCase = get(),
            saveRackUseCase = get(),
            deleteRackUseCase = get(),
        )
    }
}
