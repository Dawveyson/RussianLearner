import Foundation

// MARK: - 词条与词书

struct WordEntry: Codable, Identifiable, Hashable {
    var ru: String
    var zh: String
    var audio: String = ""
    var img: String = ""
    var note: String = ""

    var id: String { ru.lowercased() }
}

enum BookKind: String, Codable {
    case vocab = "VOCAB"
    case lesson = "LESSON"
}

struct WordBook: Identifiable, Hashable {
    var id: String
    var name: String
    var kind: BookKind
    var entries: [WordEntry]

    var hasAudio: Bool { entries.contains { !$0.audio.isEmpty } }
}

// MARK: - 教材

struct Segment: Codable, Identifiable, Hashable {
    var i: Int
    var ru: String
    var zh: String
    var dur: Double = 0
    var audio: String = ""
    var start: Double = 0
    var end: Double = 0

    var id: Int { i }
}

struct Lesson: Codable, Identifiable, Hashable {
    var n: Int
    var title: String
    var segments: [Segment]
    var audio: String = ""

    var id: Int { n }
}

// MARK: - 掌握度（轻量 SRS）
// 字段名与 Android 的 progress.json 保持一致，保证两端备份可互换。

struct Mastery: Codable, Hashable {
    var lvl: Int = 0
    var due: Double = 0
    var seen: Int = 0
    var ok: Int = 0
    var bad: Int = 0
}

// MARK: - 学习概览

struct StudyStats: Equatable {
    var totalWords: Int = 0
    var mastered: Int = 0
    var dueToday: Int = 0
    var learnedToday: Int = 0
    var streak: Int = 0
    var dailyGoal: Int = 20
    var quizBest: Int = 0
    var spellingAcc: Int = 0
}

// MARK: - 字母

struct LetterInfo: Identifiable, Hashable {
    var upper: String
    var lower: String
    var sound: String
    var sampleRu: String
    var sampleZh: String

    var id: String { upper }
}
