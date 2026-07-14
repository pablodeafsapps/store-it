package org.deafsapps.storeit.data.database

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.toUnknownDomainError

internal expect class StoreItDatabaseException : Exception

internal inline fun <T> storeItDatabaseResult(
    block: () -> T,
): Result<DomainError, T> = try {
    block().ok()
} catch (exception: StoreItDatabaseException) {
    exception.toUnknownDomainError().err()
}

internal fun Throwable.toStoreItDatabaseDomainErrorOrThrow(): DomainError.Unknown =
    if (this is StoreItDatabaseException) {
        toUnknownDomainError()
    } else {
        throw this
    }
