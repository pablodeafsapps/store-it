import SwiftUI
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif

@main
struct iOSApp: App {
    init() {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        #endif

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
