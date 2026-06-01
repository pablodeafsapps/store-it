package org.deafsapps.storeit.data.datasource

import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncStatus
import org.koin.core.annotation.Single

internal interface SyncTelemetryDataSource {
    fun onEvent(event: SyncTelemetryEvent)
}

internal sealed interface SyncTelemetryEvent {
    data class SyncStateSaved(
        val status: SyncStatus,
        val pendingOperationCount: Int,
        val hasFailureReason: Boolean,
    ) : SyncTelemetryEvent

    data class SyncStateSaveFailed(
        val requestedStatus: SyncStatus,
        val error: DomainError,
    ) : SyncTelemetryEvent

    data class SyncOperationRecorded(
        val syncOperation: SyncOperation,
    ) : SyncTelemetryEvent

    data class SyncOperationRecordFailed(
        val syncOperation: SyncOperation,
        val error: DomainError,
    ) : SyncTelemetryEvent

    data class SyncOperationDeleted(
        val operationId: String,
        val deletedCount: Long,
    ) : SyncTelemetryEvent

    data class SyncOperationDeleteFailed(
        val operationId: String,
        val error: DomainError,
    ) : SyncTelemetryEvent

    data class SyncOperationsCleared(
        val clearedCount: Long,
    ) : SyncTelemetryEvent

    data class SyncOperationsClearFailed(
        val error: DomainError,
    ) : SyncTelemetryEvent
}

@Single(binds = [SyncTelemetryDataSource::class])
internal class NoOpSyncTelemetryDataSource : SyncTelemetryDataSource {
    override fun onEvent(event: SyncTelemetryEvent) = Unit
}
