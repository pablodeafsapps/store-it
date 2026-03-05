package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.RackData
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface GetRackDataByRackIdUseCaseType : UseCase<String, Result<DomainError, RackData>>

@Factory(binds = [GetRackDataByRackIdUseCaseType::class])
internal class GetRackDataByRackIdUseCase(
    private val getRackByIdUseCase: GetRackByIdUseCaseType,
    private val getSlotsByRackIdUseCase: GetSlotsByRackIdUseCaseType,
) : GetRackDataByRackIdUseCaseType {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(input: String): Result<DomainError, RackData> =
        getRackByIdUseCase(input = input).flatMap { rack ->
            getSlotsByRackIdUseCase(input = rack.id).map { slots ->
                RackData(
                    id = Uuid.random().toString(),
                    rack = rack,
                    shelfSlots = slots,
                    items = emptyList(),
                )
            }
        }
}
