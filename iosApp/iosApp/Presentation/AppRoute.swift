import Foundation

enum AppRoute: Hashable, Codable {
    case search
    case addRack
    case addItem(initialRackId: String?, initialSlotId: String?)
    case rackDetail(rackId: String)
    case slotItems(rackId: String, slotId: String)
    case itemDetail(itemId: String)
}
