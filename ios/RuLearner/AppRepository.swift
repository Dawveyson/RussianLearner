import Foundation
import Combine

/// 全 App 单一数据源。页面只读这里的状态，写操作都通过这里落地并回流。
@MainActor
final class AppRepository: ObservableObject {
    @Published private(set) var vocabBooks: [WordBook] = []
    @Published private(set) var lessons: [Lesson] = []
    @Published private(set) var progress: [String: Mastery] = [:]
    @Published private(set) var stats = StudyStats()

    init() { reload() }

    func reload() {
        vocabBooks = BookStore.load()
        lessons = LessonStore.load()
        progress = ProgressStore.load()
        refreshStats()
    }

    /// 是否算一个「词」：至少含一个 2 字母以上的西里尔词。
    private func isWordLike(_ ru: String) -> Bool {
        ru.split(whereSeparator: { !("АЯа-яЁё".contains($0)) }).contains { $0.count >= 2 }
    }

    /// 可练习词书 = 用户导入的词库包（课文只用于随身听与查词）。
    var practiceBooks: [WordBook] { vocabBooks }

    /// 查词/搜索来源：词库 + 课文派生词。
    var lookupBooks: [WordBook] {
        vocabBooks + lessons.compactMap { l in
            let entries = l.segments
                .map { WordEntry(ru: $0.ru, zh: $0.zh, audio: $0.audio) }
                .filter { isWordLike($0.ru) }
            if entries.isEmpty { return nil }
            return WordBook(id: "lesson:\(l.n)", name: "урок \(l.n) · 第\(l.n)课",
                            kind: .lesson, entries: entries)
        }
    }

    // MARK: 词库

    @discardableResult
    func importBook(from url: URL, name: String?) -> String? {
        let r = Importer.importBook(from: url, name: name)
        reload()
        return r
    }

    @discardableResult
    func downloadBook(urlString: String, name: String?) async -> String? {
        let r = await Importer.downloadBook(urlString: urlString, name: name)
        reload()
        return r
    }

    func deleteBook(id: String) {
        BookStore.delete(id: id)
        reload()
    }

    // MARK: 教材

    func importLessons(from url: URL) -> Int {
        let n = LessonStore.importLessons(from: url)
        reload()
        return n
    }

    func importLessonZip(from url: URL) -> Int {
        let n = LessonStore.importZip(from: url)
        reload()
        return n
    }

    func deleteLesson(n: Int) {
        LessonStore.delete(n: n)
        reload()
    }

    // MARK: 查询

    /// 离线查词：先词库，再课文。
    func lookup(word: String) -> WordEntry? {
        let w = word.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !w.isEmpty else { return nil }
        for b in lookupBooks {
            if let hit = b.entries.first(where: { $0.ru.lowercased() == w }) { return hit }
        }
        return nil
    }

    /// 模糊搜索：前缀优先。
    func search(_ q: String, limit: Int = 80) -> [WordEntry] {
        let w = q.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !w.isEmpty else { return [] }
        var seen = Set<String>()
        var all: [WordEntry] = []
        for b in lookupBooks {
            for e in b.entries where seen.insert(e.ru.lowercased()).inserted { all.append(e) }
        }
        return Array(all.filter { $0.ru.lowercased().contains(w) || $0.zh.contains(w) }
            .sorted { a, b in
                let ap = a.ru.lowercased().hasPrefix(w)
                let bp = b.ru.lowercased().hasPrefix(w)
                if ap != bp { return ap }
                return a.ru.count < b.ru.count
            }
            .prefix(limit))
    }

    // MARK: 掌握度

    func masteryOf(_ ru: String) -> Mastery { ProgressStore.of(progress, ru) }

    func recordAnswer(_ ru: String, correct: Bool) {
        let next = ProgressStore.answer(progress, ru, correct: correct)
        var map = progress
        map[ru.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()] = next
        ProgressStore.save(map)
        progress = map
        Prefs.markStudied()
        refreshStats()
    }

    func resetProgress() {
        ProgressStore.clear()
        progress = [:]
        refreshStats()
    }

    /// 今日待复习 + 新词，到期优先。
    func reviewQueue(limit: Int = 40) -> [WordEntry] {
        let now = Date().timeIntervalSince1970 * 1000
        var all: [String: WordEntry] = [:]
        for b in practiceBooks {
            for e in b.entries where all[e.ru.lowercased()] == nil {
                all[e.ru.lowercased()] = e
            }
        }
        let values = Array(all.values)
        let due = values.filter { (progress[$0.ru.lowercased()]?.due ?? 0) <= now }
        let fresh = values.filter { progress[$0.ru.lowercased()] == nil }
        return Array((due.shuffled() + fresh.shuffled()).prefix(limit))
    }

    /// 「每日一词」：优先用户词库按天轮换；无词库时回落内置短句。
    func dailyWord() -> (ru: String, zh: String) {
        let slot = Int64(Date().timeIntervalSince1970 / 86_400) + Int64(Prefs.wordOffset)
        var seen = Set<String>()
        let all: [WordEntry] = practiceBooks.flatMap { $0.entries }
            .filter { seen.insert($0.ru.lowercased()).inserted }
        if !all.isEmpty {
            let e = all[indexFor(slot, all.count)]
            return (e.ru, e.zh.isEmpty ? "（暂无释义）" : e.zh)
        }
        let q = RUSSIAN_QUOTES[indexFor(slot, RUSSIAN_QUOTES.count)]
        return (q.ru, q.zh)
    }

    /// 天数偏移映射为合法下标（floorMod，负数安全）。
    private func indexFor(_ slot: Int64, _ size: Int) -> Int {
        Int((slot % Int64(size) + Int64(size)) % Int64(size))
    }

    func refreshStats() {
        var all: [String: WordEntry] = [:]
        for b in practiceBooks {
            for e in b.entries where all[e.ru.lowercased()] == nil {
                all[e.ru.lowercased()] = e
            }
        }
        let now = Date().timeIntervalSince1970 * 1000
        let sp = Prefs.spelling
        stats = StudyStats(
            totalWords: all.count,
            mastered: progress.values.filter { $0.lvl >= 4 }.count,
            dueToday: all.values.filter { (progress[$0.ru.lowercased()]?.due ?? 0) <= now }.count,
            learnedToday: Prefs.learnedToday,
            streak: Prefs.streak,
            dailyGoal: Prefs.dailyGoal,
            quizBest: Prefs.quizBest,
            spellingAcc: sp.total == 0 ? 0 : sp.correct * 100 / sp.total
        )
    }
}
