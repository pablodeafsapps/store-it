package org.deafsapps.storeit.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

internal actual fun createStoreItSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = StoreItDatabase.Schema,
    name = "storeit.db",
    onConfiguration = { config ->
        config.copy(
            extendedConfig = config.extendedConfig.copy(
                foreignKeyConstraints = true,
            ),
        )
    },
)
