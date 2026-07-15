package org.deafsapps.storeit.data.datasource

import org.koin.core.annotation.Named

/**
 * Qualifies the local SQLDelight-backed rack datasource binding.
 */
@Named
internal annotation class LocalRackDataSource

/**
 * Qualifies the remote Firebase-backed rack datasource binding.
 */
@Named
internal annotation class RemoteRackDataSource
