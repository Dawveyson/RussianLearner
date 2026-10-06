import Foundation

/// 公告与版本信息托管在 GitHub（docs/config.json），经 jsDelivr 分发。
let remoteConfigURL = "https://cdn.jsdelivr.net/gh/Dawveyson/RussianLearner@main/docs/config.json"

struct Announcement: Identifiable, Hashable {
    var id: String
    var title: String
    var body: String
    var date: String
    var level: String
}

struct RemoteConfig {
    var latestVersionCode: Int
    var latestVersionName: String
    var apkUrl: String
    var announcements: [Announcement]
}

@MainActor
final class RemoteConfigStore: ObservableObject {
    static let shared = RemoteConfigStore()

    @Published var config: RemoteConfig?
    @Published var lastError: String?

    /// 当前 App 版本号（与 Xcode MARKETING_VERSION 一致）。
    let currentVersionName = "1.0.1"
    let currentVersionCode = 10

    var hasUpdate: Bool {
        (config?.latestVersionCode ?? 0) > currentVersionCode
    }

    func refresh() async {
        guard let url = URL(string: remoteConfigURL) else { return }
        guard let text = await OnlineDict.fetch(url) else {
            lastError = "网络不可用"
            return
        }
        guard let data = text.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            lastError = "配置格式错误"
            return
        }
        var anns: [Announcement] = []
        for a in (root["announcements"] as? [[String: Any]] ?? []) {
            anns.append(Announcement(
                id: (a["id"] as? String) ?? "",
                title: (a["title"] as? String) ?? "",
                body: (a["body"] as? String) ?? "",
                date: (a["date"] as? String) ?? "",
                level: (a["level"] as? String) ?? "info"
            ))
        }
        config = RemoteConfig(
            latestVersionCode: (root["latestVersionCode"] as? Int) ?? 0,
            latestVersionName: (root["latestVersionName"] as? String) ?? "",
            apkUrl: (root["apkUrl"] as? String) ?? "",
            announcements: anns
        )
        lastError = nil
    }
}
