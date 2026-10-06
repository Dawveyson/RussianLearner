import Foundation

/// 本地学习进度（掌握度 + 全部偏好/统计）的导出 / 导入。
/// 格式与 Android 端保持一致，备份文件可跨端使用。
enum BackupStore {

    static func exportJSON() -> String {
        let progress = ProgressStore.load()
        var prog: [String: Any] = [:]
        for (k, v) in progress {
            prog[k] = ["lvl": v.lvl, "due": v.due, "seen": v.seen, "ok": v.ok, "bad": v.bad]
        }

        var root: [String: Any] = [
            "app": "RuLearner",
            "schema": 1,
            "exportedAt": isoNow(),
            "versionCode": RemoteConfigStore.shared.currentVersionCode,
            "versionName": RemoteConfigStore.shared.currentVersionName,
            "progress": prog,
            "prefs": Prefs.allValues()
        ]

        guard let data = try? JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted]),
              let text = String(data: data, encoding: .utf8) else { return "{}" }
        return text
    }

    /// 导入备份。返回是否成功。
    @discardableResult
    static func importJSON(_ text: String) -> Bool {
        guard let data = text.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return false }

        if let prog = root["progress"] as? [String: Any] {
            var map: [String: Mastery] = [:]
            for (k, v) in prog {
                guard let o = v as? [String: Any] else { continue }
                map[k] = Mastery(
                    lvl: min(max((o["lvl"] as? Int) ?? 0, 0), 5),
                    due: (o["due"] as? NSNumber)?.doubleValue ?? 0,
                    seen: (o["seen"] as? Int) ?? 0,
                    ok: (o["ok"] as? Int) ?? 0,
                    bad: (o["bad"] as? Int) ?? 0
                )
            }
            ProgressStore.save(map)
        }

        if let prefs = root["prefs"] as? [String: String] {
            Prefs.restore(prefs)
        }
        return true
    }

    /// 导出文件名：rulearn-progress-yyyyMMdd-HHmmss.json
    static func suggestedFileName() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyyMMdd-HHmmss"
        return "rulearn-progress-\(f.string(from: Date())).json"
    }

    private static func isoNow() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return f.string(from: Date())
    }
}
