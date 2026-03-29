import SwiftUI
import ComposeApp

struct RackSlotPickerView: View {
    @StateObject private var viewModelHolder: ViewModelHolder<RackSlotPickerViewModel>
    let onNavigateBack: () -> Void
    let onSlotSelectedForItem: (String, AddItemSlotVo) -> Void

    init(
        rackId: String,
        onNavigateBack: @escaping () -> Void,
        onSlotSelectedForItem: @escaping (String, AddItemSlotVo) -> Void
    ) {
        _viewModelHolder = StateObject(
            wrappedValue: ViewModelHolder(IosKoinHelper().getRackSlotPickerViewModel(rackId: rackId))
        )
        self.onNavigateBack = onNavigateBack
        self.onSlotSelectedForItem = onSlotSelectedForItem
    }

    var body: some View {
        Observing(viewModelHolder.sharedVm.uiState) { state in
            ZStack {
                if state.isLoading {
                    ProgressView()
                } else if let rack = state.rack {
                    ScrollView {
                        SlotPickerImageView(
                            photoUri: rack.photoUri,
                            slots: state.slots,
                            selectedSlotId: state.selectedSlot?.id,
                            onTap: { xRel, yRel in
                                viewModelHolder.sharedVm.onImageTap(xRel: xRel, yRel: yRel)
                            }
                        )
                        .padding()
                    }
                } else {
                    Text(state.error ?? NSLocalizedString("rack_detail_not_found", comment: ""))
                        .foregroundColor(.red)
                        .padding()
                }

                if let slot = state.selectedSlot,
                   let placement = state.selectedPlacementType,
                   let rack = state.rack {
                    VStack {
                        Spacer()
                        Button("rack_use_this_slot") {
                            onSlotSelectedForItem(
                                rack.id,
                                slot.toAddItemSlotVo(placementType: placement)
                            )
                        }
                        .buttonStyle(.borderedProminent)
                        .padding()
                    }
                }
            }
            .navigationTitle(state.rack?.name ?? NSLocalizedString("rack_title_default", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_back") { onNavigateBack() }
                }
            }
        }
    }
}

private struct SlotPickerImageView: View {
    let photoUri: String?
    let slots: [RackSlotMarkerVo]
    let selectedSlotId: String?
    let onTap: (Float, Float) -> Void

    var body: some View {
        Group {
            if let path = photoUri {
                AsyncImage(url: URL(fileURLWithPath: path)) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFit()
                    default:
                        Rectangle().fill(Color.gray.opacity(0.2)).frame(height: 200)
                    }
                }
                .frame(maxWidth: .infinity)
                .overlay(alignment: .topLeading) { overlayView }
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 200)
                    .overlay(alignment: .center) {
                        Text("rack_no_photo").foregroundColor(.secondary)
                    }
                    .overlay(alignment: .topLeading) { overlayView }
            }
        }
        .accessibilityIdentifier("rackDetailImageArea")
    }

    private var overlayView: some View {
        GeometryReader { geo in
            let w = max(geo.size.width, 1)
            let h = max(geo.size.height, 1)
            Color.clear
                .contentShape(Rectangle())
                .onTapGesture(coordinateSpace: .local) { location in
                    let xRel = Float((location.x / w).clamped(to: 0...1))
                    let yRel = Float((location.y / h).clamped(to: 0...1))
                    onTap(xRel, yRel)
                }
            ForEach(slots, id: \.id) { slot in
                Circle()
                    .fill(selectedSlotId == slot.id ? Color.accentColor : Color.accentColor.opacity(0.6))
                    .frame(width: 24, height: 24)
                    .position(x: CGFloat(slot.xRel) * geo.size.width, y: CGFloat(slot.yRel) * geo.size.height)
                    .allowsHitTesting(false)
            }
        }
    }
}
