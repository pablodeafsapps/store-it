package org.deafsapps.storeit.data.datasource

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.database.StoreItDatabaseProvider
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.koin.core.annotation.Single

@Single(binds = [SlotDataSource::class])
internal class SqlDelightSlotDataSource(
    private val databaseProvider: StoreItDatabaseProvider,
) : SlotDataSource {

    override suspend fun getSlotsByRack(rackId: String): Result<DomainError, List<ShelfSlot>> = try {
        databaseProvider.database.storeItDatabaseQueries
            .selectSlotsByRack(
                rack_id = rackId,
                mapper = { id, rowRackId, x, y, xRel, yRel ->
                    ShelfSlot(
                        id = id,
                        rackId = rowRackId,
                        position = SlotPosition(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            xRel = xRel.toFloat(),
                            yRel = yRel.toFloat(),
                        ),
                    )
                },
            )
            .executeAsList()
            .ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun saveSlot(slot: ShelfSlot): Result<DomainError, ShelfSlot> = try {
        databaseProvider.database.storeItDatabaseQueries.upsertSlot(
            id = slot.id,
            rack_id = slot.rackId,
            x = slot.position.x.toDouble(),
            y = slot.position.y.toDouble(),
            x_rel = slot.position.xRel.toDouble(),
            y_rel = slot.position.yRel.toDouble(),
        )
        slot.ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun deleteByRack(rackId: String): Result<DomainError, Unit> = try {
        databaseProvider.database.storeItDatabaseQueries.deleteSlotsByRack(rack_id = rackId)
        Unit.ok()
    } catch (_: Throwable) {
        DomainError.Unknown.err()
    }

    override suspend fun clear() {
        databaseProvider.database.storeItDatabaseQueries.deleteAllSlots()
    }
}
