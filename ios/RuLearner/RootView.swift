import SwiftUI

struct RootView: View {
    @EnvironmentObject var repo: AppRepository
    @StateObject private var remote = RemoteConfigStore.shared
    @State private var showUpdate = false

    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("首页", systemImage: "house") }
            WordsView()
                .tabItem { Label("单词", systemImage: "book") }
            AlphabetView()
                .tabItem { Label("字母", systemImage: "textformat") }
            MeView()
                .tabItem { Label("个人", systemImage: "person") }
        }
        .task {
            await remote.refresh()
            if remote.hasUpdate { showUpdate = true }
        }
        .alert("发现新版本", isPresented: $showUpdate) {
            Button("稍后") { showUpdate = false }
            if let url = URL(string: remote.config?.apkUrl ?? "") {
                Button("去下载") {
                    UIApplication.shared.open(url)
                    showUpdate = false
                }
            }
        } message: {
            let c = remote.config
            Text("最新版本：v\(c?.latestVersionName ?? "")（versionCode \(c?.latestVersionCode ?? 0)）\n当前版本：v\(remote.currentVersionName)（\(remote.currentVersionCode)）")
        }
    }
}
