package org.deafsapps.storeit.data.local.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

internal actual fun createStoreItSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = StoreItDatabase.Schema,
    name = "storeit.db",
)
