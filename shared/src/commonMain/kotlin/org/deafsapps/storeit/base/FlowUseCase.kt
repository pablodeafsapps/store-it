package org.deafsapps.storeit.base

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for flowable use cases in the application.
 * Use cases represent single business actions and should be lightweight.
 */
interface FlowUseCase<in Input, out Output> {
    /**
     * Executes the use case with the given input and returns a flow of results.
     * 
     * @param input The input parameters for the use case
     * @return The result of the use case execution
     */
    operator fun invoke(input: Input): Flow<Output>
}
