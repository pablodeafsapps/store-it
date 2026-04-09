import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinInitKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    #if DEBUG
                    try? await IosKoinHelper().preloadDebugMockDataIfNeeded(isDebugBuild: true)
                    #endif
                }
        }
    }
}
