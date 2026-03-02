import SwiftUI

struct RackDetailPlaceholderView: View {
    let rackId: String
    let onNavigateBack: () -> Void

    var body: some View {
        NavigationView {
            VStack {
                Text("Rack: \(rackId) (detail screen coming in T029)")
                    .foregroundColor(.secondary)
                    .padding()
                Spacer()
            }
            .navigationTitle("Rack detail")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") {
                        onNavigateBack()
                    }
                }
            }
        }
    }
}
