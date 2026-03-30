package org.deafsapps.storeit.data.local.db

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal actual fun createStoreItSqlDriver(): SqlDriver {
    val context = object : KoinComponent {}.get<android.content.Context>()
    return AndroidSqliteDriver(
        schema = StoreItDatabase.Schema,
        context = context,
        name = "storeit.db",
    )
}
