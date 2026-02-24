package org.deafsapps.storeit.presentation.rack.viewmodel

import org.deafsapps.storeit.data.repository.InMemoryRackRepository
import org.deafsapps.storeit.domain.usecase.SaveRackUseCase
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel

object AddRackViewModelFactory {
    private val rackRepository = InMemoryRackRepository()
    private val saveRackUseCase = SaveRackUseCase(rackRepository = rackRepository)

    fun createViewModel(): AddRackViewModel {
        return AddRackViewModel(saveRackUseCase = saveRackUseCase)
    }
}
