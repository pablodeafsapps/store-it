package org.deafsapps.storeit.domain.model

import kotlinx.coroutines.CancellationException
import org.deafsapps.storeit.base.Error

/**
 * Describes domain-level failures that can be returned by repositories and use cases.
 */
sealed interface DomainError : Error {
    /**
     * Signals an unexpected failure that could not be mapped to a more specific domain error.
     */
    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: Throwable? = null,
    ) : DomainError

    /**
     * Signals that a requested resource does not exist.
     */
    data class NotFound(val resource: String, val id: String? = null) : DomainError {
        override val message: String = "Resource '$resource'${id?.let { " with id '$it'" } ?: ""} not found"
        override val cause: Throwable? = null
    }

    /**
     * Signals that input data failed domain validation.
     */
    data class ValidationError(val field: String? = null, val reason: String) : DomainError {
        override val message: String =
            field?.let { "Validation error for field '$it': $reason" } ?: "Validation error: $reason"
        override val cause: Throwable? = null
    }
}

internal fun Throwable.toUnknownDomainError(
    message: String = this.message ?: "Unknown error",
): DomainError.Unknown {
    if (this is CancellationException) {
        throw this
    }

    return DomainError.Unknown(
        message = message,
        cause = this,
    )
}
