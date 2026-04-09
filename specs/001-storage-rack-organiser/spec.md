# Feature Specification: Store it! — Storage Rack Organiser & Item Locator

**Feature Branch**: `001-storage-rack-organiser`  
**Created**: 2025-02-10  
**Status**: Draft  
**Input**: User description: "Develop Store it!, an organiser/locator platform for individuals' and families' belongings. The application should allow users to register storage racks and fill them with their things or items." Plus user journey (register rack → add item → locate item; progressive persistence; no login in MVP).

## Clarifications

### Session 2025-02-10

- Q: Should the app include mock/dummy data for debugging and UI assessment? → A: Yes; include 1–5 mock/dummy records (at least one rack and some items) so a rack and items can be initially consulted and the UI duly assessed during development.

### Session 2026-04-09

- Q: What is the MVP default order for racks and search results? → A: Racks are shown with most recently changed first, then by name as a stable tie-breaker. Search results prioritise item-name matches before description-only matches, then use the same recency rule.
- Q: How is a shelf slot identified in MVP? → A: A single tap creates or selects a slot anchored to the stored tap coordinates on the rack image; the persisted slot record is the source of truth.
- Q: What happens when camera capture or gallery picking fails or is cancelled? → A: Saving is blocked until the user provides a valid photo, and the UI must show a recoverable validation or picker error state without losing already entered metadata.
- Q: What is the non-functional stance for MVP? → A: Accessibility and performance are required at a practical MVP level, but no advanced compliance programme or benchmarking beyond the success criteria is part of this feature.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register a storage rack (Priority: P1)

The user registers a storage rack so they can later add items to it. The app requires at least one registered rack before any items can be added. The user captures a photo of the rack (camera or device gallery), then may add a name/identifier, description, location, and other relevant information. The rack image is stored and used later as an interactive map: the user can tap on regions (e.g. shelf slots) to assign items to those positions. Multiple items may be placed on the same slot.

**Why this priority**: Without at least one rack, the user cannot add or locate items; registration is the foundation of the product.

**Independent Test**: User can create one rack with a photo and metadata, see it in the list of racks, and open it to see the rack image. Delivers value as the first usable step (rack inventory).

**Acceptance Scenarios**:

1. **Given** the user has no racks, **When** they open the option to add a rack and capture (or pick) a photo and enter a name, **Then** a new rack is created and appears in their rack list.
2. **Given** the user is adding a rack, **When** they optionally add description and location, **Then** that information is stored and shown when viewing the rack.
3. **Given** at least one rack exists, **When** the user opens a rack, **Then** they see the rack image and can tap on it to define or use shelf slots.
4. **Given** the user has one or more racks, **When** they view the rack list, **Then** they can open a rack, edit its metadata (name, description, location), or remove a rack after confirmation, and the list is shown with the most recently changed rack first.

---

### User Story 2 - Add an item to a rack (Priority: P2)

The user adds an item and places it on a specific rack and slot. They capture a photo of the item (camera or gallery), optionally add name, description, quantity, owner, tags, etc., then choose the rack and tap the shelf slot on the rack image to place the item. The flow may start by selecting the rack and slot first, then adding the item photo and metadata; the end result is the same (item associated with that rack and slot).

**Why this priority**: Adding items is the core “organiser” value once racks exist.

**Independent Test**: User selects a rack, taps a slot on the rack image, adds an item with at least a photo (and optionally name/description), and the item appears in that slot. Can be verified by opening the rack and tapping the same slot to see the item.

**Acceptance Scenarios**:

1. **Given** at least one rack exists, **When** the user starts “add item” and captures (or picks) a photo and enters a name, **Then** they can select a rack and tap a slot to place the item.
2. **Given** the user is adding an item, **When** they optionally add description, quantity, owner, or tags, **Then** that information is stored and shown when viewing the item.
3. **Given** the user has chosen a rack and tapped a slot, **When** they complete the item photo and metadata, **Then** the item is associated with that rack and slot.
4. **Given** the user starts from a rack and taps a slot, **When** they then add item photo and metadata, **Then** the item is associated with that rack and slot (reverse flow).
5. **Given** a slot already has items, **When** the user places another item on the same slot, **Then** the new item is added to that slot (multiple items per slot allowed).

---

### User Story 3 - Locate an item and view or edit it (Priority: P3)

The user locates an item by opening a rack, tapping a shelf slot on the rack image, and seeing the list of items in that slot. They can tap an item to view or edit its data. A search box is always available: the user can type and search across all items by name and description to find where an item is stored.

**Why this priority**: “Locator” value and quick access to item details; builds on P1 and P2.

**Independent Test**: User opens a rack, taps a slot that has items, sees the list, taps one item and views/edits it. User can also search by text and get results that include the item and its location (rack/slot). Verifiable without login or remote sync.

**Acceptance Scenarios**:

1. **Given** a rack with at least one slot that has items, **When** the user opens the rack and taps that slot, **Then** they see the list of items in that slot.
2. **Given** the user sees the list of items in a slot, **When** they tap an item, **Then** they can view all stored data (name, description, photo, quantity, owner, tags, etc.).
3. **Given** the user is viewing an item, **When** they choose to edit, **Then** they can change name, description, quantity, owner, tags, and other editable fields.
4. **Given** the user is anywhere in the app, **When** they use the search box and type text, **Then** matching items (by name and description) are shown with the rack name and slot identifier or coordinates needed to identify the location.
5. **Given** the user searched and got results, **When** they tap a result, **Then** they can navigate to that item (and thus its rack and slot).

---

### Edge Cases

- What happens when the user has no racks yet and tries to add an item? The system MUST guide them to create at least one rack first (or disable “add item” until a rack exists).
- What happens when the user taps an empty slot (no items)? The system MUST show an empty state and allow adding an item to that slot.
- What happens when the user does not provide a photo for a rack or item? For MVP, photo is REQUIRED for both rack and item; the system MUST show a validation message and prevent saving until a photo is provided.
- What happens when search returns no results? The system MUST show a clear “no results” state and not imply that data was lost.
- How does the system handle very long names, descriptions, or many tags? The system MUST accept and display them without data loss; truncation or scrolling is acceptable for display.
- What happens when the user removes the last item from a slot or deletes a rack? The system MUST update the model and UI so the slot or rack no longer shows that item or rack. When the user deletes a rack, the system MUST ask for confirmation, then delete the rack and all associated slots and items (cascade). Empty racks (no items) are allowed.
- What happens when camera capture or gallery selection fails, is denied, or is cancelled? The system MUST keep the draft metadata, show a recoverable error or validation state, and prevent saving until a valid photo is attached.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to register at least one storage rack with one required photo (camera or gallery) plus metadata limited to name, description, and location for MVP.
- **FR-002**: System MUST use the rack photo as an interactive map so users can tap to create or select shelf slots anchored to persisted image coordinates.
- **FR-003**: System MUST allow multiple items per shelf slot.
- **FR-004**: System MUST allow users to add an item with one required photo (camera or gallery) plus metadata limited to name, description, quantity, owner, and tags for MVP, and to place it on a chosen rack and slot (by tapping the rack image).
- **FR-005**: System MUST support both flows: “add item then choose rack/slot” and “choose rack/slot then add item”; outcome MUST be equivalent.
- **FR-006**: System MUST allow users to open a rack, tap a slot, and see the list of items in that slot.
- **FR-007**: System MUST allow users to tap an item to view and edit its data (name, description, photo, quantity, owner, tags, etc.).
- **FR-008**: System MUST provide a search box available at all times that searches items by name and description and returns results that show rack name plus slot identifier or coordinates; results MUST prioritise name matches before description-only matches.
- **FR-009**: System MUST persist all data in a way that survives app restart; persistence strategy will be introduced progressively (no persistence in first iteration, then local storage, then remote).
- **FR-010**: System MUST NOT require login or sign-up for the MVP; user/account association is out of scope for this feature.
- **FR-011**: System MUST provide or support 1–5 mock/dummy records (at least one rack and some items) for debugging and UI assessment; these MAY be preloaded in non-production builds or toggled for development, and MUST be excluded from production startup data.
- **FR-012**: Rack lists MUST default to most recently changed first, with name used as a stable tie-breaker when timestamps are equal.
- **FR-013**: If photo capture or gallery selection fails, is cancelled, or permission is denied, the system MUST preserve entered metadata, show a recoverable error state, and block saving until a valid photo is present.

### Key Entities

- **Rack**: A storage unit (e.g. shelf, cabinet) represented by a single photo and metadata limited to name, description, and location for MVP. The photo acts as a map for slot placement.
- **Shelf slot**: A persisted point on a rack image created from a user tap and identified by stored coordinates plus a stable slot id; can hold zero or more items.
- **Item**: A belonging stored in a specific rack and slot; has a photo and metadata (name, description, quantity, owner, tags, etc.). Same item is not duplicated across slots for MVP (one placement per item).

## Assumptions

- **Persistence**: FR-009 is a product requirement. The implementation strategy is progressive persistence: first delivery may keep data in memory only; later iterations add local then remote persistence.
- **Authentication**: No login/sign-up in MVP; all users are effectively “local single user.” Multi-device or multi-user sync is out of scope for this feature.
- **Owner field**: “Owner” for an item is a free-text field (e.g. family member name); no separate user or account entity in MVP.
- **Rack image slots**: Slots are defined by the user tapping on the rack image; the system stores the tap coordinates and a derived slot identifier. Exact UX for drawing or selecting slot boundaries is left to design, but tap-to-place is the MVP minimum.
- **Photo requirements**: Rack and item each require one primary photo in MVP. Camera and gallery are both supported.
- **Mock/debug data**: At least one rack and some items (1–5 mock records in total) MUST be available for debugging and UI assessment (e.g. preloaded or toggled in development builds).
- **Accessibility**: MVP requires clear labels, discoverable actions, and platform-standard accessibility support for primary flows; a formal accessibility audit is out of scope for this feature.
- **Performance**: MVP performance is measured by SC-001 through SC-003. No separate load-testing or benchmarking programme is required for this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user who has never used the app can register their first rack (photo + name) in under two minutes without instructions.
- **SC-002**: A user can add an item and place it on an existing rack slot in under three minutes.
- **SC-003**: A user can locate an item by searching by name or description and open its details in under 30 seconds.
- **SC-004**: At least 90% of users who complete onboarding (one rack, one item) can successfully find that item again via the rack map or search. Sample size and validation method belong to the MVP test plan or validation protocol, not this feature spec.
- **SC-005**: Data entered by the user (racks, items, metadata) is retained across app restarts once persistence is implemented; until then, in-memory behaviour is acceptable for the first iteration.
