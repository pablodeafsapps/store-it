package org.deafsapps.storeit.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal actual fun createStoreItSqlDriver(): SqlDriver {
    val context = object : KoinComponent {}.get<android.content.Context>()
    return AndroidSqliteDriver(
        schema = StoreItDatabase.Schema,
        context = context,
        name = "storeit.db",
        callback = object : AndroidSqliteDriver.Callback(StoreItDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
}
