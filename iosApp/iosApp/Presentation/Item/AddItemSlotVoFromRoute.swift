import Shared

/// Maps serialized `AppRoute` values into typed `AddItemSlotVo`. String parsing stays at the navigation boundary only.
enum AddItemSlotVoFromRoute {
    static var empty: AddItemSlotVo {
        AddItemSlotVo(id: nil, placementType: nil, xRel: nil, yRel: nil)
    }

    static func existing(slotId: String) -> AddItemSlotVo {
        AddItemSlotVo(id: slotId, placementType: SlotPlacementType.existing, xRel: nil, yRel: nil)
    }

    /// Invalid numbers yield nil coordinates; `AddItemViewModel` surfaces "Draft slot position is missing" on save.
    static func draft(slotId: String, xRel: String, yRel: String) -> AddItemSlotVo {
        let x: KotlinFloat? = Double(xRel).map { KotlinFloat(float: Float($0)) }
        let y: KotlinFloat? = Double(yRel).map { KotlinFloat(float: Float($0)) }
        return AddItemSlotVo(
            id: slotId,
            placementType: SlotPlacementType.draft,
            xRel: x,
            yRel: y
        )
    }
}
