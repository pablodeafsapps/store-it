import Foundation

enum AppRoute: Hashable, Codable {
    case account
    case search
    case addRack
    case addItem
    case addItemAtSlot(initialRackId: String, initialSlotId: String)
    case addItemAtDraftSlot(initialRackId: String, initialSlotId: String, initialSlotXRel: String, initialSlotYRel: String)
    case rackDetail(rackId: String)
    case slotItems(rackId: String, slotId: String)
    case itemDetail(itemId: String)
}
