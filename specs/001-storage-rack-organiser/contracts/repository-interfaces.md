# Repository Contracts (Domain Layer)

**Feature**: 001-storage-rack-organiser  
**Date**: 2025-02-10

The data layer is accessed via repository interfaces defined in the **domain** layer. Implementations may use in-memory, local DB, or (future) remote backend. Backend (e.g. Firebase) is a **placeholder**: abstract behind these interfaces so it can be swapped or added later.

## RackRepository

- **getAllRacks()**: Return all racks (e.g. `Flow<List<Rack>>` or suspend `List<Rack>`).
- **getRackById(id)**: Return one rack by id, or null/None if not found.
- **saveRack(rack)**: Create or update a rack; return success/failure (e.g. `Either<DomainError, Rack>`).
- **deleteRack(id)**: Remove rack and its slots and item placements; return success/failure.

## ItemRepository (or RackItemRepository)

- **getItemsByRack(rackId)**: Return all items for a rack (optionally grouped by slot).
- **getItemsBySlot(rackId, slotId)**: Return all items in a given slot.
- **getItemById(id)**: Return one item by id.
- **searchItems(query)**: Search by name and description; return items with enough info to identify rack and slot (per FR-008).
- **saveItem(item)**: Create or update item (placement = rackId + slotId); return success/failure.
- **deleteItem(id)**: Remove item; return success/failure.

## Slot / placement

Slots may be created implicitly on first tap (tap-to-place). Options:

- **ShelfSlot** as first-class entity with id, rackId, position; or
- **Placement** as (rackId, slotPosition) where slotPosition is coordinates or logical index.

Contract: “Get items by slot” and “place item on slot” are required; exact type (ShelfSlot vs coordinates) is an implementation detail behind the repository.

## Remote backend placeholder

- **Decision**: Do not implement Firebase (or other remote) in MVP. Reuse the existing `RackDataSource` and `ItemDataSource` interfaces and provide Firebase placeholder implementations that are currently no-op or stub.
- **Future**: When adding Firebase (or alternative), implement these sources and keep repository interface unchanged so domain and UI stay backend-agnostic.
- **Current placeholder**: `FirebaseRackDataSource` and `FirebaseItemDataSource` live under `composeApp/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/` and implement the existing datasource contracts with stubbed no-op behavior so the app remains local-first.
- **Firebase wiring**: Replace the stub logic in those datasource implementations with Firebase-backed logic, or swap the active datasource implementation in DI when remote sync is enabled. Keep `RackRepository` and `ItemRepository` as the stable domain-facing contracts.
