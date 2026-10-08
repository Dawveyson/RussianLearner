import Foundation

/// App 数据目录（对应 Android 的 filesDir）。
enum AppFiles {
    static var dir: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    static func url(_ name: String) -> URL { dir.appendingPathComponent(name) }

    static func read(_ name: String) -> String? {
        try? String(contentsOf: url(name), encoding: .utf8)
    }

    @discardableResult
    static func write(_ name: String, _ text: String) -> Bool {
        let u = url(name)
        try? FileManager.default.createDirectory(at: u.deletingLastPathComponent(), withIntermediateDirectories: true)
        do {
            try text.write(to: u, atomically: true, encoding: .utf8)
            return true
        } catch {
            return false
        }
    }
}

// MARK: - 掌握度（SRS）

/// 轻量间隔重复：按「俄语原文小写」为键，跨词书共享掌握度。
/// level 0..5，对应复习间隔 0/1/2/4/7/15 天。
enum ProgressStore {
    private static let intervals: [Double] = [0, 1, 2, 4, 7, 15]
    private static let file = "progress.json"

    static func load() -> [String: Mastery] {
        guard let text = AppFiles.read(file),
              let data = text.data(using: .utf8),
              let raw = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        else { return [:] }

        var out: [String: Mastery] = [:]
        for (k, v) in raw {
            guard let o = v as? [String: Any] else { continue }
            out[k] = Mastery(
                lvl: min(max((o["lvl"] as? Int) ?? 0, 0), 5),
                due: (o["due"] as? NSNumber)?.doubleValue ?? 0,
                seen: (o["seen"] as? Int) ?? 0,
                ok: (o["ok"] as? Int) ?? 0,
                bad: (o["bad"] as? Int) ?? 0
            )
        }
        return out
    }

    static func save(_ map: [String: Mastery]) {
        var obj: [String: Any] = [:]
        for (k, v) in map {
            obj[k] = ["lvl": v.lvl, "due": v.due, "seen": v.seen, "ok": v.ok, "bad": v.bad]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: obj),
              let text = String(data: data, encoding: .utf8) else { return }
        AppFiles.write(file, text)
    }

    static func of(_ map: [String: Mastery], _ ru: String) -> Mastery {
        map[ru.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()] ?? Mastery()
    }

    /// 答对升级、答错降级，返回新记录。
    static func answer(_ map: [String: Mastery], _ ru: String, correct: Bool) -> Mastery {
        let key = ru.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let cur = map[key] ?? Mastery()
        let level = correct ? min(cur.lvl + 1, 5) : max(cur.lvl - 1, 0)
        return Mastery(
            lvl: level,
            due: Date().addingTimeInterval(intervals[level] * 86_400).timeIntervalSince1970 * 1000,
            seen: cur.seen + 1,
            ok: cur.ok + (correct ? 1 : 0),
            bad: cur.bad + (correct ? 0 : 1)
        )
    }

    static func clear() {
        try? FileManager.default.removeItem(at: AppFiles.url(file))
    }
}

// MARK: - 词库

/// 词书持久化。用户词库全部来自导入 / 下载。
enum BookStore {
    private static let file = "vocab.json"

    static func load() -> [WordBook] {
        guard let text = AppFiles.read(file),
              let data = text.data(using: .utf8),
              let arr = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]]
        else { return [] }

        var out: [WordBook] = []
        for o in arr {
            let id = (o["id"] as? String) ?? ""
            if id.isEmpty { continue }
            let entries = entriesFrom(o["entries"] as? [[String: Any]])
            if entries.isEmpty { continue }
            out.append(WordBook(id: id, name: (o["name"] as? String) ?? id, kind: .vocab, entries: entries))
        }
        return out
    }

    static func save(_ books: [WordBook]) {
        let arr: [[String: Any]] = books.filter { $0.kind == .vocab }.map { b in
            let ea: [[String: Any]] = b.entries.map {
                ["ru": $0.ru, "zh": $0.zh, "audio": $0.audio, "img": $0.img, "note": $0.note]
            }
            return ["id": b.id, "name": b.name, "entries": ea]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: arr),
              let text = String(data: data, encoding: .utf8) else { return }
        AppFiles.write(file, text)
    }

    private static func entriesFrom(_ arr: [[String: Any]]?) -> [WordEntry] {
        guard let arr else { return [] }
        return arr.compactMap { e in
            let ru = ((e["ru"] as? String) ?? "").trimmingCharacters(in: .whitespaces)
            if ru.isEmpty { return nil }
            return WordEntry(
                ru: ru,
                zh: (e["zh"] as? String) ?? "",
                audio: (e["audio"] as? String) ?? "",
                img: (e["img"] as? String) ?? "",
                note: (e["note"] as? String) ?? ""
            )
        }
    }

    @discardableResult
    static func addBook(_ entries: [WordEntry], name: String) -> String {
        let id = "b_\(Int(Date().timeIntervalSince1970 * 1000))"
        var books = load()
        books.append(WordBook(id: id, name: name, kind: .vocab, entries: entries))
        save(books)
        return name
    }

    static func delete(id: String) {
        save(load().filter { $0.id != id })
    }

    /// 解析 JSON 数组或 TSV/CSV 文本。
    static func parse(_ text: String) -> [WordEntry] {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasPrefix("[") { return parseJSON(t) }
        return parseTable(t)
    }

    private static func parseJSON(_ t: String) -> [WordEntry] {
        guard let data = t.data(using: .utf8) else { return [] }
        var arr: [[String: Any]] = []
        if let a = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]] {
            arr = a
        } else if let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] {
            for k in ["words", "data", "list", "items"] {
                if let a = root[k] as? [[String: Any]] { arr = a; break }
            }
        }

        var out: [String: WordEntry] = [:]
        var order: [String] = []
        for e in arr {
            let ru = (((e["ru"] as? String) ?? (e["word"] as? String) ?? (e["text"] as? String)) ?? "")
                .trimmingCharacters(in: .whitespaces)
            if ru.isEmpty { continue }
            let zh = (((e["zh"] as? String) ?? (e["mean"] as? String) ?? (e["meaning"] as? String) ?? (e["trans"] as? String) ?? (e["translation"] as? String)) ?? "")
                .trimmingCharacters(in: .whitespaces)
            if zh.isEmpty { continue }
            let key = ru.lowercased()
            if out[key] != nil { continue }
            out[key] = WordEntry(
                ru: ru,
                zh: zh,
                audio: (e["audio"] as? String) ?? "",
                img: (e["img"] as? String) ?? (e["image"] as? String) ?? "",
                note: (e["note"] as? String) ?? ""
            )
            order.append(key)
        }
        return order.compactMap { out[$0] }
    }

    private static func parseTable(_ t: String) -> [WordEntry] {
        var out: [String: WordEntry] = [:]
        var order: [String] = []
        for (idx, raw) in t.components(separatedBy: .newlines).enumerated() {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty { continue }
            let low = line.lowercased()
            if idx == 0 && (low == "ru" || low.hasPrefix("ru\t") || low.hasPrefix("ru,")
                            || low.hasPrefix("ru;") || low.hasPrefix("ru ")) { continue }

            let parts: [String]
            if line.contains("\t") { parts = line.components(separatedBy: "\t") }
            else if line.contains(",") { parts = line.components(separatedBy: ",") }
            else if line.contains(";") { parts = line.components(separatedBy: ";") }
            else { continue }
            if parts.count < 2 { continue }
            let ru = parts[0].trimmingCharacters(in: .whitespaces)
            let zh = parts[1].trimmingCharacters(in: .whitespaces)
            if ru.isEmpty || zh.isEmpty { continue }
            let key = ru.lowercased()
            if out[key] != nil { continue }
            out[key] = WordEntry(
                ru: ru,
                zh: zh,
                audio: parts.count > 2 ? parts[2].trimmingCharacters(in: .whitespaces) : "",
                img: parts.count > 3 ? parts[3].trimmingCharacters(in: .whitespaces) : ""
            )
            order.append(key)
        }
        return order.compactMap { out[$0] }
    }
}

// MARK: - 教材

/// 教材（课文）持久化。支持 {"lessons":[...]} 与裸数组两种形状。
enum LessonStore {
    private static let file = "lessons.json"
    private static let manifestNames: Set<String> = ["book.json", "lessons.json", "manifest.json", "index.json"]

    static func load() -> [Lesson] {
        guard let text = AppFiles.read(file) else { return [] }
        return parseText(text)
    }

    static func save(_ lessons: [Lesson]) {
        let arr: [[String: Any]] = lessons.map { l in
            let segs: [[String: Any]] = l.segments.map {
                ["i": $0.i, "ru": $0.ru, "zh": $0.zh, "dur": $0.dur,
                 "audio": $0.audio, "start": $0.start, "end": $0.end]
            }
            return ["n": l.n, "title": l.title, "audio": l.audio, "segments": segs]
        }
        guard let data = try? JSONSerialization.data(withJSONObject: arr),
              let text = String(data: data, encoding: .utf8) else { return }
        AppFiles.write(file, text)
    }

    static func merge(_ base: [Lesson], _ incoming: [Lesson]) -> [Lesson] {
        var byN: [Int: Lesson] = [:]
        for l in base { byN[l.n] = l }
        for l in incoming { byN[l.n] = l }
        return byN.values.sorted { $0.n < $1.n }
    }

    /// 从本地课程 JSON 导入，同课号覆盖。返回导入课数。
    @discardableResult
    static func importLessons(from url: URL) -> Int {
        guard let text = try? String(contentsOf: url, encoding: .utf8) else { return 0 }
        let parsed = parseText(text)
        if parsed.isEmpty { return 0 }
        save(merge(load(), parsed))
        return parsed.count
    }

    /// 从 zip 音频书导入：解压 → 找清单 → 解析 → 改写音频绝对路径 → 按课号合并。
    @discardableResult
    static func importZip(from url: URL) -> Int {
        let fm = FileManager.default
        let dest = AppFiles.dir.appendingPathComponent("books/\(Int(Date().timeIntervalSince1970 * 1000))")
        guard unzip(url, to: dest) else { return 0 }
        guard let manifest = findManifest(in: dest),
              let text = try? String(contentsOf: manifest, encoding: .utf8) else { return 0 }
        let parsed = parseText(text)
        if parsed.isEmpty { return 0 }
        let resolved = resolveAssets(parsed, base: dest)
        save(merge(load(), resolved))
        _ = fm
        return resolved.count
    }

    static func delete(n: Int) {
        save(load().filter { $0.n != n })
    }

    private static func findManifest(in dir: URL) -> URL? {
        guard let e = FileManager.default.enumerator(at: dir, includingPropertiesForKeys: [.isRegularFileKey]) else { return nil }
        var firstJSON: URL?
        for case let url as URL in e {
            let name = url.lastPathComponent.lowercased()
            if manifestNames.contains(name),
               (try? url.resourceValues(forKeys: [.isRegularFileKey]).isRegularFile) == true {
                return url
            }
            // 兜底：记住第一个普通 .json 文件，实在没匹配到已知清单名时用它
            if firstJSON == nil && name.hasSuffix(".json")
                && (try? url.resourceValues(forKeys: [.isRegularFileKey]).isRegularFile) == true {
                firstJSON = url
            }
        }
        return firstJSON
    }

    /// 解压 zip，带 zip-slip 防护：所有目标必须落在 dest 内。
    private static func unzip(_ src: URL, to dest: URL) -> Bool {
        let fm = FileManager.default
        let destRoot = dest.standardizedFileURL.path

        // iOS Foundation 不提供 zip 解压，这里用自实现的 Importer/Inflater 展开。
        guard let stage = Importer.makeStage(for: dest) else { return false }
        defer { try? fm.removeItem(at: stage) }
        do {
            try fm.createDirectory(at: dest, withIntermediateDirectories: true)
        } catch {
            return false
        }

        guard Importer.expand(src, to: stage) else { return false }

        guard let e = fm.enumerator(at: stage, includingPropertiesForKeys: nil) else { return false }
        for case let url as URL in e {
            let rel = url.path.replacingOccurrences(of: stage.path, with: "")
            let target = dest.appendingPathComponent(rel)
            let path = target.standardizedFileURL.path
            // zip slip 防护
            guard path == destRoot || path.hasPrefix(destRoot + "/") else { continue }
            try? fm.removeItem(at: target)
            try? fm.moveItem(at: url, to: target)
        }
        return true
    }

    private static func resolveAssets(_ lessons: [Lesson], base: URL) -> [Lesson] {
        lessons.map { l in
            var nl = l
            nl.audio = resolveOne(l.audio, base: base)
            nl.segments = l.segments.map { s in
                var ns = s
                ns.audio = resolveOne(s.audio, base: base)
                return ns
            }
            return nl
        }
    }

    private static func resolveOne(_ path: String, base: URL) -> String {
        if path.isEmpty { return "" }
        if path.hasPrefix("http://") || path.hasPrefix("https://")
            || path.hasPrefix("/") || path.hasPrefix("file://") { return path }
        let f = base.appendingPathComponent(path)
        return FileManager.default.fileExists(atPath: f.path) ? f.path : path
    }

    static func parseText(_ text: String) -> [Lesson] {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let data = t.data(using: .utf8) else { return [] }
        var arr: [[String: Any]] = []
        if t.hasPrefix("[") {
            arr = (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]] ?? []
        } else if t.hasPrefix("{") {
            let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
            for k in ["lessons", "data", "list"] {
                if let a = root[k] as? [[String: Any]] { arr = a; break }
            }
        }

        var out: [Lesson] = []
        for (i, o) in arr.enumerated() {
            let n = (o["n"] as? Int) ?? (i + 1)
            let title = (o["title"] as? String) ?? "урок \(n)"
            let lessonAudio = (o["audio"] as? String) ?? ""
            let segs = o["segments"] as? [[String: Any]] ?? []
            var segments: [Segment] = []
            for s in segs {
                let ru = (((s["ru"] as? String) ?? (s["text"] as? String)) ?? "")
                    .trimmingCharacters(in: .whitespaces)
                if ru.isEmpty { continue }
                let start = (s["start"] as? NSNumber)?.doubleValue ?? 0
                let end = (s["end"] as? NSNumber)?.doubleValue ?? 0
                let dur = (s["dur"] as? NSNumber)?.doubleValue ?? 0
                segments.append(Segment(
                    i: (s["i"] as? Int) ?? (segments.count + 1),
                    ru: ru,
                    zh: (s["zh"] as? String) ?? (s["mean"] as? String) ?? (s["translation"] as? String) ?? "",
                    dur: dur > 0 ? dur : (end - start),
                    audio: (s["audio"] as? String) ?? "",
                    start: start,
                    end: end
                ))
            }
            if !segments.isEmpty {
                out.append(Lesson(n: n, title: title, segments: segments, audio: lessonAudio))
            }
        }
        return out
    }
}
