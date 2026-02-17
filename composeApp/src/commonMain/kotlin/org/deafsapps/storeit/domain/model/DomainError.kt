package org.deafsapps.storeit.domain.model

import org.deafsapps.storeit.base.Error

internal sealed interface DomainError : Error {
    data object Unknown : DomainError {
        override val message: String = "Unknown error"
        override val cause: Throwable? = null
    }
    
    data class NotFound(val resource: String, val id: String? = null) : DomainError {
        override val message: String = "Resource '$resource'${id?.let { " with id '$it'" } ?: ""} not found"
        override val cause: Throwable? = null
    }
    
    data class ValidationError(val field: String? = null, val reason: String) : DomainError {
        override val message: String =
            field?.let { "Validation error for field '$it': $reason" } ?: "Validation error: $reason"
        override val cause: Throwable? = null
    }
}