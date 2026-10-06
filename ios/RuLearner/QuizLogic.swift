import Foundation

/// 归一化：小写、去两端空白、压缩内部空白。
func norm(_ s: String) -> String {
    let lowered = s.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    return lowered.split(whereSeparator: { $0.isWhitespace }).joined(separator: " ")
}

enum QuizMode: String, CaseIterable {
    case zh2ru = "中译俄"
    case ru2zh = "俄译中"
    case spell = "拼写"
}

struct Question: Identifiable {
    var mode: QuizMode
    var prompt: String
    var options: [String]
    var answerIndex: Int
    var entry: WordEntry

    var id: String { entry.id + mode.rawValue }
}

/// 从练习词书构造一组题目，mode 均匀分配，默认 4 选 1。
func buildQuestions(books: [WordBook], count: Int, choices: Int = 4) -> [Question] {
    var seen = Set<String>()
    let practice: [WordEntry] = books.flatMap { $0.entries }
        .filter { seen.insert($0.ru.lowercased()).inserted }
    guard !practice.isEmpty else { return [] }

    let picked = Array(practice.shuffled().prefix(count))
    let modes = QuizMode.allCases

    return picked.enumerated().map { i, e in
        let mode = modes[i % modes.count]
        if mode == .spell {
            return Question(mode: mode, prompt: e.zh, options: [], answerIndex: 0, entry: e)
        }
        let answerIsRu = (mode == .ru2zh)
        let correct = answerIsRu ? e.ru : e.zh
        let pool = practice.filter { $0.id != e.id }.shuffled()
        let distractors = pool.prefix(max(choices - 1, 0)).map { answerIsRu ? $0.ru : $0.zh }
        var opts = [correct] + distractors
        opts.shuffle()
        let idx = opts.firstIndex(of: correct) ?? 0
        return Question(mode: mode, prompt: answerIsRu ? e.ru : e.zh,
                        options: opts, answerIndex: idx, entry: e)
    }
}
