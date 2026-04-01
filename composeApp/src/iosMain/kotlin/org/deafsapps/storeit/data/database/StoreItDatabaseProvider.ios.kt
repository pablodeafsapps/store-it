package org.deafsapps.storeit.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.deafsapps.storeit.data.database.StoreItDatabase

internal actual fun createStoreItSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = StoreItDatabase.Schema,
    name = "storeit.db",
)
