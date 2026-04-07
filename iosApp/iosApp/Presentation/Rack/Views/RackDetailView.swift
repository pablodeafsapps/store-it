import SwiftUI
import ComposeApp
import UIKit

struct RackDetailView: View {
    @State private var viewModelSessionId = UUID()
    @Binding var navigationPath: [AppRoute]
    let rackId: String
    let onNavigateBack: () -> Void
    let onAddItemHere: ((String, String, String, String) -> Void)?
    let onNavigateToSlotItems: ((String, String) -> Void)?


    var body: some View {
        RackDetailScreen(
            rackId: rackId,
            onNavigateBack: onNavigateBack,
            onAddItemHere: onAddItemHere,
            onNavigateToSlotItems: onNavigateToSlotItems,
        )
        .id(viewModelSessionId)
        .onChange(of: navigationPath.count) { oldCount, newCount in
            if newCount < oldCount {
                viewModelSessionId = UUID()
            }
        }
    }

    init(
        navigationPath: Binding<[AppRoute]>,
        rackId: String,
        onNavigateBack: @escaping () -> Void,
        onAddItemHere: ((String, String, String, String) -> Void)? = nil,
        onNavigateToSlotItems: ((String, String) -> Void)? = nil,
    ) {
        self._navigationPath = navigationPath
        self.rackId = rackId
        self.onNavigateBack = onNavigateBack
        self.onAddItemHere = onAddItemHere
        self.onNavigateToSlotItems = onNavigateToSlotItems
    }
}

private struct RackDetailScreen: View {
    @StateObject private var rackDetailViewModel: ViewModelHolder<RackDetailViewModel>
    let onNavigateBack: () -> Void
    let onAddItemHere: ((String, String, String, String) -> Void)?
    let onNavigateToSlotItems: ((String, String) -> Void)?

    init(
        rackId: String,
        onNavigateBack: @escaping () -> Void,
        onAddItemHere: ((String, String, String, String) -> Void)? = nil,
        onNavigateToSlotItems: ((String, String) -> Void)? = nil
    ) {
        _rackDetailViewModel = StateObject(
            wrappedValue: ViewModelHolder(IosKoinHelper().getRackDetailViewModel(rackId: rackId))
        )
        self.onNavigateBack = onNavigateBack
        self.onAddItemHere = onAddItemHere
        self.onNavigateToSlotItems = onNavigateToSlotItems
    }

    var body: some View {
        Observing(rackDetailViewModel.sharedVm.uiState) { state in
            RackDetailContent(
                state: state,
                onImageTap: { xRel, yRel in
                    rackDetailViewModel.sharedVm.onImageTap(xRel: xRel, yRel: yRel)
                },
                onEditSelected: rackDetailViewModel.sharedVm.onEditSelected,
                onRemoveRackSelected: rackDetailViewModel.sharedVm.onRemoveRackSelected,
                onDismissEditDialog: rackDetailViewModel.sharedVm.onDismissEditDialog,
                onUpdateEditName: rackDetailViewModel.sharedVm.onUpdateEditName,
                onUpdateEditDescription: rackDetailViewModel.sharedVm.onUpdateEditDescription,
                onUpdateEditLocation: rackDetailViewModel.sharedVm.onUpdateEditLocation,
                onSaveRackEdits: rackDetailViewModel.sharedVm.onSaveRackEdits,
                onDismissDeleteConfirm: rackDetailViewModel.sharedVm.onDismissDeleteConfirm,
                onConfirmDeleteRack: rackDetailViewModel.sharedVm.onConfirmDeleteRack,
                onSlotMarkerDrag: { slotId, xRel, yRel in
                    rackDetailViewModel.sharedVm.onSlotMarkerDrag(
                        slotId: slotId,
                        xRel: xRel,
                        yRel: yRel,
                    )
                },
                onSaveSlotMarkerPosition: { slotId, xRel, yRel in
                    rackDetailViewModel.sharedVm.onSaveSlotMarkerPosition(
                        slotId: slotId,
                        xRel: xRel,
                        yRel: yRel,
                    )
                }
            )
        }
        .task {
            for await event in rackDetailViewModel.sharedVm.uiEvent {
                guard let event else { continue }
                if event is RackDetailUiEventNavigateBack {
                    onNavigateBack()
                } else if let nav = event as? RackDetailUiEventNavigateToSlotItems,
                          let onNavigateToSlotItems {
                    onNavigateToSlotItems(nav.rackId, nav.slotId)
                } else if let add = event as? RackDetailUiEventNavigateToAddItemDraft,
                          let onAddItemHere {
                    onAddItemHere(
                        add.rackId,
                        add.slotId,
                        String(add.slotXRel),
                        String(add.slotYRel),
                    )
                }
            }
        }
    }
}

private struct RackDetailContent: View {
    let state: RackDetailUiState
    let onImageTap: (Float, Float) -> Void
    let onEditSelected: () -> Void
    let onRemoveRackSelected: () -> Void
    let onDismissEditDialog: () -> Void
    let onUpdateEditName: (String) -> Void
    let onUpdateEditDescription: (String) -> Void
    let onUpdateEditLocation: (String) -> Void
    let onSaveRackEdits: () -> Void
    let onDismissDeleteConfirm: () -> Void
    let onConfirmDeleteRack: () -> Void
    let onSlotMarkerDrag: (String, Float, Float) -> Void
    let onSaveSlotMarkerPosition: (String, Float, Float) -> Void
    @State private var pendingDragConfirmation: PendingDragConfirmation?
    @State private var showSlotMoveConfirm = false

    var body: some View {
        ZStack {
            if state.isLoading {
                ProgressView().scaleEffect(1.2)
            } else if let rack = state.rack {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        RackImageView(
                            photoUri: rack.photoUri,
                            slots: state.slots,
                            selectedSlotId: nil,
                            onTap: onImageTap,
                            onSlotDrag: { slotId, xRel, yRel in
                                onSlotMarkerDrag(slotId, xRel, yRel)
                            },
                            onSlotDragFinished: { slotId, initialXRel, initialYRel, finalXRel, finalYRel in
                                pendingDragConfirmation = PendingDragConfirmation(
                                    slotId: slotId,
                                    initialXRel: initialXRel,
                                    initialYRel: initialYRel,
                                    finalXRel: finalXRel,
                                    finalYRel: finalYRel
                                )
                                showSlotMoveConfirm = true
                            }
                        )
                        Text(state.slots.isEmpty ? "rack_browse_hint_no_slots" : "rack_browse_hint_with_slots")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .accessibilityIdentifier("rackBrowseSlotHint")
                        if !rack.description_.isEmpty {
                            Text(rack.description_).font(.body)
                        }
                        if !rack.location.isEmpty {
                            Text(String(format: NSLocalizedString("rack_location_prefix", comment: ""), rack.location))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                }
            } else {
                Text(state.error ?? NSLocalizedString("rack_detail_not_found", comment: ""))
                    .foregroundColor(.red)
                    .padding()
            }
        }
        .navigationTitle(state.rack?.name ?? NSLocalizedString("rack_title_default", comment: ""))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button("rack_edit_button") { onEditSelected() }
                        .accessibilityIdentifier("editRackMenuItem")
                    Button("rack_remove_button", role: .destructive) { onRemoveRackSelected() }
                        .accessibilityIdentifier("removeRackMenuItem")
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityIdentifier("rackDetailMenuButton")
            }
        }
        .sheet(isPresented: Binding(
            get: { state.showEditDialog },
            set: { if !$0 { onDismissEditDialog() } }
        )) {
            EditRackSheet(
                name: state.editName,
                description: state.editDescription,
                location: state.editLocation,
                onNameChange: onUpdateEditName,
                onDescriptionChange: onUpdateEditDescription,
                onLocationChange: onUpdateEditLocation,
                onDismiss: onDismissEditDialog,
                onSave: onSaveRackEdits
            )
        }
        .alert("rack_remove_confirm_title", isPresented: Binding(
            get: { state.showDeleteConfirm },
            set: { if !$0 { onDismissDeleteConfirm() } }
        )) {
            Button("common_cancel", role: .cancel) { onDismissDeleteConfirm() }
            Button("common_remove", role: .destructive) { onConfirmDeleteRack() }
        } message: {
            Text("rack_remove_confirm_message")
        }
        .alert("rack_slot_move_confirm_title", isPresented: $showSlotMoveConfirm) {
            Button("common_cancel", role: .cancel) {
                if let pendingDragConfirmation {
                    onSlotMarkerDrag(
                        pendingDragConfirmation.slotId,
                        pendingDragConfirmation.initialXRel,
                        pendingDragConfirmation.initialYRel
                    )
                }
                pendingDragConfirmation = nil
            }
            Button("common_save") {
                if let pendingDragConfirmation {
                    onSaveSlotMarkerPosition(
                        pendingDragConfirmation.slotId,
                        pendingDragConfirmation.finalXRel,
                        pendingDragConfirmation.finalYRel
                    )
                }
                pendingDragConfirmation = nil
            }
        } message: {
            Text("rack_slot_move_confirm_message")
        }
    }
}

private struct RackImageView: View {
    let photoUri: String?
    let slots: [RackSlotMarkerVo]
    let selectedSlotId: String?
    let onTap: (Float, Float) -> Void
    let onSlotDrag: (String, Float, Float) -> Void
    let onSlotDragFinished: (String, Float, Float, Float, Float) -> Void

    var body: some View {
        Group {
            if let path = photoUri {
                AsyncImage(url: URL(fileURLWithPath: path)) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFit()
                    default:
                        Rectangle().fill(Color.gray.opacity(0.2)).frame(height: 200).overlay(alignment: .center) { ProgressView() }
                    }
                }
                .frame(maxWidth: .infinity)
                .overlay(alignment: .topLeading) { overlayView }
                .accessibilityIdentifier("rackDetailImageArea")
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 200)
                    .overlay(alignment: .center) { Text("rack_no_photo").foregroundColor(.secondary) }
                    .overlay(alignment: .topLeading) { overlayView }
                    .accessibilityIdentifier("rackDetailImageArea")
            }
        }
    }

    private var overlayView: some View {
        GeometryReader { geo in
            let w = max(geo.size.width, 1)
            let h = max(geo.size.height, 1)
            ZStack(alignment: .topLeading) {
                Color.clear
                    .frame(width: w, height: h)
                    .contentShape(Rectangle())
                    .onTapGesture { location in
                        let xRel = Float((location.x / w).clamped(to: 0...1))
                        let yRel = Float((location.y / h).clamped(to: 0...1))
                        onTap(xRel, yRel)
                    }
                ForEach(slots, id: \.id) { slot in
                    RackSlotMarkerView(
                        slot: slot,
                        selectedSlotId: selectedSlotId,
                        width: w,
                        height: h,
                        onTap: onTap,
                        onSlotDrag: onSlotDrag,
                        onSlotDragFinished: onSlotDragFinished
                    )
                }
            }
            .frame(width: w, height: h)
            .coordinateSpace(name: "rackImage")
        }
    }

}

private struct RackSlotMarkerView: View {
    private static let markerSizeRest: CGFloat = 24
    private static let markerSizeDragging: CGFloat = 28
    private static let longPressDuration: TimeInterval = 0.35
    private static let positionChangeTolerance: Float = 0.001
    /// Full pulse period (s); half ≈ 0.32 s to align with Android `tween(320ms)`.
    private static let flashPeriod: TimeInterval = 0.64

    let slot: RackSlotMarkerVo
    let selectedSlotId: String?
    let width: CGFloat
    let height: CGFloat
    let onTap: (Float, Float) -> Void
    let onSlotDrag: (String, Float, Float) -> Void
    let onSlotDragFinished: (String, Float, Float, Float, Float) -> Void

    @State private var isDragVisualActive = false

    var body: some View {
        ZStack {
            if isDragVisualActive {
                TimelineView(.animation(minimumInterval: 1.0 / 60.0, paused: false)) { context in
                    let t = context.date.timeIntervalSinceReferenceDate
                    let s = sin(t * 2 * .pi / Self.flashPeriod)
                    let alpha = 0.4 + 0.6 * (0.5 + 0.5 * s)
                    Circle()
                        .fill(selectedSlotId == slot.id ? Color.accentColor : Color.accentColor.opacity(0.6))
                        .frame(width: Self.markerSizeDragging, height: Self.markerSizeDragging)
                        .opacity(alpha)
                }
            } else {
                Circle()
                    .fill(selectedSlotId == slot.id ? Color.accentColor : Color.accentColor.opacity(0.6))
                    .frame(width: Self.markerSizeRest, height: Self.markerSizeRest)
            }

            RackSlotMarkerGestureOverlay(
                slot: slot,
                rackWidth: width,
                rackHeight: height,
                longPressDuration: Self.longPressDuration,
                onTap: {
                    onTap(slot.xRel, slot.yRel)
                },
                onDragBegan: {
                    isDragVisualActive = true
                },
                onDragChanged: { xRel, yRel in
                    onSlotDrag(slot.id, xRel, yRel)
                },
                onDragEnded: { initialXRel, initialYRel, finalXRel, finalYRel in
                    isDragVisualActive = false

                    guard hasMeaningfulPositionChange(
                        initialXRel: initialXRel,
                        initialYRel: initialYRel,
                        finalXRel: finalXRel,
                        finalYRel: finalYRel,
                    ) else {
                        onSlotDrag(slot.id, initialXRel, initialYRel)
                        return
                    }

                    onSlotDragFinished(
                        slot.id,
                        initialXRel,
                        initialYRel,
                        finalXRel,
                        finalYRel,
                    )
                },
                onDragCancelled: { initialXRel, initialYRel in
                    isDragVisualActive = false
                    onSlotDrag(slot.id, initialXRel, initialYRel)
                },
            )
            .frame(width: Self.markerSizeDragging, height: Self.markerSizeDragging)
        }
        .position(x: CGFloat(slot.xRel) * width, y: CGFloat(slot.yRel) * height)
    }

    private func hasMeaningfulPositionChange(
        initialXRel: Float,
        initialYRel: Float,
        finalXRel: Float,
        finalYRel: Float,
    ) -> Bool {
        abs(initialXRel - finalXRel) > Self.positionChangeTolerance ||
            abs(initialYRel - finalYRel) > Self.positionChangeTolerance
    }
}

private struct RackSlotMarkerGestureOverlay: UIViewRepresentable {
    let slot: RackSlotMarkerVo
    let rackWidth: CGFloat
    let rackHeight: CGFloat
    let longPressDuration: TimeInterval
    let onTap: () -> Void
    let onDragBegan: () -> Void
    let onDragChanged: (Float, Float) -> Void
    let onDragEnded: (Float, Float, Float, Float) -> Void
    let onDragCancelled: (Float, Float) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        view.isOpaque = false

        let tapRecognizer = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleTap),
        )

        let longPressRecognizer = UILongPressGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleLongPress(_:)),
        )
        longPressRecognizer.minimumPressDuration = longPressDuration
        longPressRecognizer.allowableMovement = .greatestFiniteMagnitude
        tapRecognizer.require(toFail: longPressRecognizer)

        view.addGestureRecognizer(tapRecognizer)
        view.addGestureRecognizer(longPressRecognizer)
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.parent = self
    }

    final class Coordinator: NSObject {
        var parent: RackSlotMarkerGestureOverlay
        private var dragStartXRel: Float?
        private var dragStartYRel: Float?
        private var dragStartLocation: CGPoint?

        init(parent: RackSlotMarkerGestureOverlay) {
            self.parent = parent
        }

        @objc func handleTap() {
            parent.onTap()
        }

        @objc func handleLongPress(_ recognizer: UILongPressGestureRecognizer) {
            let location = recognizer.location(in: recognizer.view)

            switch recognizer.state {
            case .began:
                dragStartXRel = parent.slot.xRel
                dragStartYRel = parent.slot.yRel
                dragStartLocation = location
                parent.onDragBegan()
                parent.onDragChanged(parent.slot.xRel, parent.slot.yRel)
            case .changed:
                guard let dragStartXRel, let dragStartYRel, let dragStartLocation else {
                    return
                }

                let translationX = location.x - dragStartLocation.x
                let translationY = location.y - dragStartLocation.y
                let xRel = translatedX(
                    initialXRel: dragStartXRel,
                    translationX: translationX,
                    width: parent.rackWidth,
                )
                let yRel = translatedY(
                    initialYRel: dragStartYRel,
                    translationY: translationY,
                    height: parent.rackHeight,
                )
                parent.onDragChanged(xRel, yRel)
            case .ended:
                guard let dragStartXRel, let dragStartYRel, let dragStartLocation else {
                    return
                }

                let translationX = location.x - dragStartLocation.x
                let translationY = location.y - dragStartLocation.y
                let finalXRel = translatedX(
                    initialXRel: dragStartXRel,
                    translationX: translationX,
                    width: parent.rackWidth,
                )
                let finalYRel = translatedY(
                    initialYRel: dragStartYRel,
                    translationY: translationY,
                    height: parent.rackHeight,
                )

                parent.onDragEnded(dragStartXRel, dragStartYRel, finalXRel, finalYRel)
                reset()
            case .cancelled, .failed:
                guard let dragStartXRel, let dragStartYRel else {
                    return
                }

                parent.onDragCancelled(dragStartXRel, dragStartYRel)
                reset()
            default:
                break
            }
        }

        private func reset() {
            dragStartXRel = nil
            dragStartYRel = nil
            dragStartLocation = nil
        }

        private func translatedX(
            initialXRel: Float,
            translationX: CGFloat,
            width: CGFloat,
        ) -> Float {
            let delta = Float(translationX / max(width, 1))
            return (initialXRel + delta).clamped(to: 0...1)
        }

        private func translatedY(
            initialYRel: Float,
            translationY: CGFloat,
            height: CGFloat,
        ) -> Float {
            let delta = Float(translationY / max(height, 1))
            return (initialYRel + delta).clamped(to: 0...1)
        }
    }
}

private struct PendingDragConfirmation {
    let slotId: String
    let initialXRel: Float
    let initialYRel: Float
    let finalXRel: Float
    let finalYRel: Float
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

private struct EditRackSheet: View {
    let name: String
    let description: String
    let location: String
    let onNameChange: (String) -> Void
    let onDescriptionChange: (String) -> Void
    let onLocationChange: (String) -> Void
    let onDismiss: () -> Void
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                TextField("item_name_label", text: Binding(get: { name }, set: onNameChange))
                TextField("rack_description_label", text: Binding(get: { description }, set: onDescriptionChange), axis: .vertical)
                    .lineLimit(3...6)
                TextField("rack_location_label", text: Binding(get: { location }, set: onLocationChange))
            }
            .navigationTitle("rack_edit_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common_cancel") { onDismiss() }.accessibilityIdentifier("editRackCancelButton")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("common_save") { onSave() }.accessibilityIdentifier("editRackSaveButton")
                }
            }
        }
    }
}
