import Foundation

/// 导入辅助：词库 / 课程 / zip 音频书。
enum Importer {

    /// 创建临时暂存目录。
    static func makeStage(for dest: URL) -> URL? {
        let stage = dest.deletingLastPathComponent()
            .appendingPathComponent("stage-\(UUID().uuidString)")
        do {
            try FileManager.default.createDirectory(at: stage, withIntermediateDirectories: true)
            return stage
        } catch {
            return nil
        }
    }

    /// 把 zip 展开到目录（含 zip-slip 防护）。
    @discardableResult
    static func expand(_ zip: URL, to dir: URL) -> Bool {
        let root = dir.standardizedFileURL.path
        let entries = Zip.entries(of: zip)
        guard !entries.isEmpty else { return false }
        for e in entries {
            let target = dir.appendingPathComponent(e.path).standardizedFileURL
            // zip slip：拒绝 ../ 越界路径
            guard target.path == root || target.path.hasPrefix(root + "/") else { continue }
            try? FileManager.default.createDirectory(at: target.deletingLastPathComponent(),
                                                    withIntermediateDirectories: true)
            try? e.data.write(to: target, options: .atomic)
        }
        return true
    }

    /// 从文件导入词书，返回词书名。
    @discardableResult
    static func importBook(from url: URL, name: String?) -> String? {
        guard let text = try? String(contentsOf: url, encoding: .utf8) else { return nil }
        let entries = BookStore.parse(text)
        if entries.isEmpty { return nil }
        let fallback = url.deletingPathExtension().lastPathComponent
        let finalName = (name?.isEmpty == false) ? name! : (fallback.isEmpty ? "导入词库" : fallback)
        return BookStore.addBook(entries, name: finalName)
    }

    /// 从 URL 下载词库。
    static func downloadBook(urlString: String, name: String?) async -> String? {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed),
              url.scheme?.hasPrefix("http") == true,
              let (data, _) = try? await URLSession.shared.data(from: url),
              let text = String(data: data, encoding: .utf8) else { return nil }
        let entries = BookStore.parse(text)
        if entries.isEmpty { return nil }
        var fallback = url.lastPathComponent
        for suffix in [".json", ".tsv", ".txt"] where fallback.hasSuffix(suffix) {
            fallback = String(fallback.dropLast(suffix.count))
            break
        }
        let finalName = (name?.isEmpty == false) ? name! : (fallback.isEmpty ? "下载词库" : fallback)
        return BookStore.addBook(entries, name: finalName)
    }
}
