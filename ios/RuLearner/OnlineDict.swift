import Foundation
import CryptoKit

/// 联网查词结果。
struct OnlineMeaning {
    var translation: String
    var phonetic: String? = nil
}

/// 国内可访问的俄中在线查词（有道）。未配置密钥时走演示接口。
enum OnlineDict {

    static func lookup(word: String, appKey: String, appSecret: String) async -> OnlineMeaning? {
        if !appKey.isEmpty, !appSecret.isEmpty,
           let r = await lookupSigned(word: word, appKey: appKey, appSecret: appSecret) {
            return r
        }
        return await lookupDemo(word: word)
    }

    static func lookupDemo(word: String) async -> OnlineMeaning? {
        guard var c = URLComponents(string: "https://aidemo.youdao.com/trans") else { return nil }
        c.queryItems = [
            URLQueryItem(name: "q", value: word),
            URLQueryItem(name: "from", value: "ru"),
            URLQueryItem(name: "to", value: "zh-CHS")
        ]
        guard let url = c.url, let json = await fetch(url) else { return nil }
        return parse(json, withBasic: false)
    }

    private static func lookupSigned(word: String, appKey: String, appSecret: String) async -> OnlineMeaning? {
        let salt = String(UInt64(Date().timeIntervalSince1970 * 1_000_000_000))
        let curtime = String(Int(Date().timeIntervalSince1970))
        let input = word.count <= 20
            ? word
            : String(word.prefix(10)) + String(word.count) + String(word.suffix(10))
        let sign = sha256(appKey + input + salt + curtime + appSecret)

        guard var c = URLComponents(string: "https://openapi.youdao.com/api") else { return nil }
        c.queryItems = [
            URLQueryItem(name: "q", value: word),
            URLQueryItem(name: "from", value: "ru"),
            URLQueryItem(name: "to", value: "zh-CHS"),
            URLQueryItem(name: "appKey", value: appKey),
            URLQueryItem(name: "salt", value: salt),
            URLQueryItem(name: "sign", value: sign),
            URLQueryItem(name: "signType", value: "v3"),
            URLQueryItem(name: "curtime", value: curtime)
        ]
        guard let url = c.url, let json = await fetch(url) else { return nil }
        return parse(json, withBasic: true)
    }

    private static func parse(_ json: String, withBasic: Bool) -> OnlineMeaning? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              ((root["errorCode"] as? String) ?? "") == "0",
              let arr = root["translation"] as? [Any],
              let tr = arr.first as? String, !tr.isEmpty else { return nil }
        var phonetic: String? = nil
        if withBasic, let basic = root["basic"] as? [String: Any] {
            let p = (basic["phonetic"] as? String) ?? ""
            if !p.isEmpty { phonetic = p }
        }
        return OnlineMeaning(translation: tr, phonetic: phonetic)
    }

    /// 简单 GET，10 秒超时。
    static func fetch(_ url: URL) async -> String? {
        var req = URLRequest(url: url)
        req.timeoutInterval = 10
        req.setValue("RuLearner/1.0", forHTTPHeaderField: "User-Agent")
        req.setValue("https://www.youdao.com/", forHTTPHeaderField: "Referer")
        guard let (data, _) = try? await URLSession.shared.data(for: req) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func sha256(_ s: String) -> String {
        SHA256.hash(data: Data(s.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
