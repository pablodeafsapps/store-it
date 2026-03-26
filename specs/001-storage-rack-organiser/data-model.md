# Data Model: Store it! — Storage Rack Organiser

**Feature**: 001-storage-rack-organiser  
**Date**: 2025-02-10

## Entities

### Rack

- **Description**: A storage unit (shelf, cabinet, etc.) represented by one photo and metadata. The photo is used as an interactive map; slots are defined by user tap positions.
- **Attributes**:
  - **id**: Unique identifier (e.g. UUID).
  - **name**: User-facing name/identifier (optional in spec; recommend required for display).
  - **description**: Optional free text.
  - **location**: Optional free text (e.g. "Garage", "Room 2").
  - **photoUri** (or equivalent): Reference to stored image (camera or gallery). One primary photo per rack.
  - **createdAt** / **updatedAt**: Optional timestamps for ordering/audit.
- **Validation**: At least one of name or photo recommended for MVP; id non-empty.
- **Relationships**: Has many shelf slots (logical regions on the image). Deleting a rack implies removing all slots and item placements for that rack.

### ShelfSlot

- **Description**: A region on a rack’s image (e.g. a shelf) where items can be placed. Identified by coordinates or a logical position.
- **Attributes**:
  - **id**: Unique identifier (e.g. UUID or composite rackId + position).
  - **rackId**: Reference to parent Rack.
  - **position**: Coordinates (e.g. x, y normalised or pixel) or logical index. Exact representation is implementation-defined (tap-to-place minimum).
- **Validation**: rackId required; position must be within rack image bounds (or valid logical index).
- **Relationships**: Belongs to one Rack; has zero or more Items. Same slot can hold multiple items (FR-003).

### Item

- **Description**: A belonging stored in a specific rack and slot; one photo and metadata.
- **Attributes**:
  - **id**: Unique identifier.
  - **rackId**: Rack where the item is placed.
  - **slotId**: Shelf slot within that rack (or equivalent position).
  - **name**: Optional free text.
  - **description**: Optional free text.
  - **photoUri**: Reference to item image (camera or gallery). One primary photo per item.
  - **quantity**: Optional numeric value (e.g. integer) when applicable.
  - **owner**: Optional free text (e.g. family member name).
  - **tags**: Optional list of strings for grouping/filtering.
  - **createdAt** / **updatedAt**: Optional.
- **Validation**: rackId and slotId required for placement; id non-empty. Long names/descriptions/tags accepted; display may truncate or scroll (spec edge case).
- **Relationships**: Belongs to one Rack and one ShelfSlot. One placement per item in MVP (no duplicate item across slots). Same slot can have many items.

## State / lifecycle

- **Rack**: Create → (optional) Edit metadata / Replace photo → Delete. Delete removes associated slots and item placements.
- **ShelfSlot**: Created implicitly when user taps on rack image (tap-to-place). No explicit “delete slot” in spec; slot identity is derived from tap.
- **Item**: Create (with photo + optional metadata + rack/slot) → View → Edit → Delete. Moving item to another slot = update slotId (and optionally rackId if cross-rack).

## Search

- **Scope**: All items. Fields used: name, description (spec FR-008). Results must allow user to identify and navigate to item (and thus rack + slot).
- **No requirement** for rack/slot ordering in list views; can be implementation choice (e.g. by name, by date).

## Persistence

- **First iteration**: In-memory only; no persistence. Mock data (1–5 records) for debugging/UI.
- **Later**: Local persistence (full CRUD for Rack, ShelfSlot, Item); then remote abstraction (Firebase placeholder) behind repository interfaces.
