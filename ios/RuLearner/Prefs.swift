import Foundation

/// 轻量偏好存储（对应 Android 的 SharedPreferences "rulearn_prefs"）。
/// 键名与 Android 保持一致，方便备份文件跨端读取。
enum Prefs {
    private static var d: UserDefaults { UserDefaults.standard }

    private static let name = "rulearn_prefs"
    static let storeName = name

    // MARK: 每日目标

    static var dailyGoal: Int {
        get { d.integer(forKey: "daily_goal") == 0 ? 20 : d.integer(forKey: "daily_goal") }
        set { d.set(min(max(newValue, 5), 200), forKey: "daily_goal") }
    }

    // MARK: 打卡

    static var streak: Int {
        get { d.integer(forKey: "streak") }
        set { d.set(newValue, forKey: "streak") }
    }

    static var lastStudyDay: String {
        get { d.string(forKey: "last_study_day") ?? "" }
        set { d.set(newValue, forKey: "last_study_day") }
    }

    /// 每日内容的偏移量（点「换一个」时 +1）。
    static var wordOffset: Int {
        get { d.integer(forKey: "word_offset") }
        set { d.set(newValue, forKey: "word_offset") }
    }

    @discardableResult
    static func bumpWordOffset() -> Int {
        let v = wordOffset + 1
        d.set(v, forKey: "word_offset")
        return v
    }

    /// 本地日期 yyyy-MM-dd。
    static func todayKey(_ date: Date = Date()) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    /// 今日已学（跨天自动归零）。
    static var learnedToday: Int {
        lastStudyDay == todayKey() ? d.integer(forKey: "learned_today") : 0
    }

    /// 记录一次学习：维护今日计数与连续打卡天数。
    static func markStudied() {
        let today = todayKey()
        let last = lastStudyDay
        if last != today {
            let yesterday = todayKey(Date().addingTimeInterval(-86_400))
            let nextStreak = (last == yesterday) ? streak + 1 : 1
            d.set(learnedToday + 1, forKey: "learned_today")
            d.set(nextStreak, forKey: "streak")
            d.set(today, forKey: "last_study_day")
        } else {
            d.set(learnedToday + 1, forKey: "learned_today")
        }
    }

    // MARK: 默写累计

    static var spelling: (correct: Int, total: Int) {
        (d.integer(forKey: "sp_correct"), d.integer(forKey: "sp_total"))
    }

    static func recordSpelling(correct: Bool) {
        let s = spelling
        d.set(s.correct + (correct ? 1 : 0), forKey: "sp_correct")
        d.set(s.total + 1, forKey: "sp_total")
    }

    // MARK: 刷题最佳

    static var quizBest: Int {
        get { d.integer(forKey: "quiz_best") }
        set { d.set(newValue, forKey: "quiz_best") }
    }

    static func setQuizBest(_ v: Int) {
        if v > quizBest { d.set(v, forKey: "quiz_best") }
    }

    // MARK: 有道智云密钥

    static var apiKey: String {
        get { d.string(forKey: "yd_appkey") ?? "" }
        set { d.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "yd_appkey") }
    }

    static var apiSecret: String {
        get { d.string(forKey: "yd_secret") ?? "" }
        set { d.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "yd_secret") }
    }

    // MARK: 发音

    static var preferNetworkTTS: Bool {
        get { d.object(forKey: "prefer_net_tts") as? Bool ?? true }
        set { d.set(newValue, forKey: "prefer_net_tts") }
    }

    // MARK: 当前词书

    static var activeBook: String {
        get { d.string(forKey: "active_book") ?? "" }
        set { d.set(newValue, forKey: "active_book") }
    }

    // MARK: 备份用：全部键值

    static func allValues() -> [String: String] {
        var out: [String: String] = [:]
        for (k, v) in d.dictionaryRepresentation() where k != name {
            out[k] = String(describing: v)
        }
        return out
    }

    static func restore(_ values: [String: String]) {
        for (k, v) in values { d.set(v, forKey: k) }
    }
}
