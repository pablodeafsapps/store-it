package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.data.datasource.SyncOperationDataSource
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal interface SyncOperationRepository {
    suspend fun enqueueCreate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String? = null,
    ): Result<DomainError, SyncOperation>

    suspend fun enqueueUpdate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String? = null,
    ): Result<DomainError, SyncOperation>

    suspend fun enqueueDelete(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String? = null,
    ): Result<DomainError, SyncOperation>
}

@Single(binds = [SyncOperationRepository::class])
internal class DefaultSyncOperationRepository(
    private val syncOperationDataSource: SyncOperationDataSource,
    private val operationIdGenerator: SyncOperationIdGenerator,
) : SyncOperationRepository {
    override suspend fun enqueueCreate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> = enqueueOperation(
        accountId = accountId,
        entityType = entityType,
        entityId = entityId,
        payloadJson = payloadJson,
        operationType = SyncOperationType.Create,
    )

    override suspend fun enqueueUpdate(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> = enqueueOperation(
        accountId = accountId,
        entityType = entityType,
        entityId = entityId,
        payloadJson = payloadJson,
        operationType = SyncOperationType.Update,
    )

    override suspend fun enqueueDelete(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
    ): Result<DomainError, SyncOperation> = enqueueOperation(
        accountId = accountId,
        entityType = entityType,
        entityId = entityId,
        payloadJson = payloadJson,
        operationType = SyncOperationType.Delete,
    )

    private suspend fun enqueueOperation(
        accountId: String?,
        entityType: SyncEntityType,
        entityId: String,
        payloadJson: String?,
        operationType: SyncOperationType,
    ): Result<DomainError, SyncOperation> {
        if (entityId.isBlank()) {
            return DomainError.ValidationError(
                field = "entityId",
                reason = "Entity ID cannot be blank",
            ).err()
        }

        return syncOperationDataSource.saveSyncOperation(
            syncOperation = SyncOperation(
                id = operationIdGenerator.generate(),
                accountId = accountId,
                entityType = entityType,
                entityId = entityId,
                operationType = operationType,
                payloadJson = payloadJson,
                syncStatus = SyncOperationStatus.Pending,
            ),
        )
    }
}

internal fun interface SyncOperationIdGenerator {
    fun generate(): String
}

@Single(binds = [SyncOperationIdGenerator::class])
internal class DefaultSyncOperationIdGenerator : SyncOperationIdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun generate(): String = Uuid.random().toString()
}
