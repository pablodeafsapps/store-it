package org.deafsapps.storeit.data.local.db

import app.cash.sqldelight.db.SqlDriver
import org.koin.core.annotation.Single

internal expect fun createStoreItSqlDriver(): SqlDriver

@Single
internal class StoreItDatabaseProvider {
    internal val database: StoreItDatabase by lazy(LazyThreadSafetyMode.NONE) {
        StoreItDatabase(driver = createStoreItSqlDriver())
    }
}
