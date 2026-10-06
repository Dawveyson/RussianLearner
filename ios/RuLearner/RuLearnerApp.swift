import SwiftUI

@main
struct RuLearnerApp: App {
    @StateObject private var repo = AppRepository()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(repo)
        }
    }
}
