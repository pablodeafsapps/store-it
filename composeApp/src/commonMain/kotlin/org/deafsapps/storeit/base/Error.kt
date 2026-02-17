package org.deafsapps.storeit.base

/**
 * A base interface representing a generic error within the application.
 *
 * Implement this interface to create specific error types that can be used
 * throughout the system, providing a common abstraction for error handling.
 */
internal interface Error {
    val message: String
    val cause: Throwable?
}
