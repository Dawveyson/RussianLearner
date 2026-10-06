import SwiftUI
import UniformTypeIdentifiers

struct ImportView: View {
    @EnvironmentObject var repo: AppRepository
    @State private var msg = ""
    @State private var urlText = ""
    @State private var bookName = ""
    @State private var downloading = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if !msg.isEmpty {
                    Text(msg)
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .glassEffect(.regular.tint(.accentColor), in: .rect(cornerRadius: 14))
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("导入词库（本地文件）").font(.headline)
                        Text("支持 .json / .tsv / .txt / .csv。JSON 形如 [{\"ru\":\"...\",\"zh\":\"...\"}]，"
                             + "或用 Tab / 逗号分隔的两列。")
                            .font(.caption).foregroundStyle(.secondary)
                        Button {
                            importing = true
                        } label: { Label("选择词库文件", systemImage: "doc.badge.plus") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("下载词库（按 URL）").font(.headline)
                        Text("填入放在任意可访问托管上的词库文件地址。")
                            .font(.caption).foregroundStyle(.secondary)
                        TextField("https://…/vocab.json", text: $urlText)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textFieldStyle(.plain)
                            .padding(12)
                            .background(.thinMaterial, in: .rect(cornerRadius: 10))
                        TextField("词书名（可选）", text: $bookName)
                            .textFieldStyle(.plain)
                            .padding(12)
                            .background(.thinMaterial, in: .rect(cornerRadius: 10))
                        Button {
                            downloading = true
                            Task {
                                let name = await repo.downloadBook(
                                    urlString: urlText, name: bookName.isEmpty ? nil : bookName)
                                downloading = false
                                msg = name.map { "已添加词书：\($0)" } ?? "下载失败（地址不可访问或格式不符）"
                            }
                        } label: {
                            if downloading { ProgressView() } else { Label("下载词库", systemImage: "arrow.down.circle") }
                        }
                        .buttonStyle(GlassPrimaryButtonStyle())
                        .disabled(downloading || urlText.isEmpty)
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("导入 zip 音频书（推荐）").font(.headline)
                        Text("清单 + 音频打成一个包。包内放 book.json（或 lessons.json / manifest.json / index.json），"
                             + "音频放在 audio/ 等子目录，清单里写相对路径。")
                            .font(.caption).foregroundStyle(.secondary)
                        Button {
                            importingZip = true
                        } label: { Label("选择 zip 包", systemImage: "archivebox") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("导入课程 JSON（不带音频）").font(.headline)
                        Text("只有课文和可选外链音频时用这个。")
                            .font(.caption).foregroundStyle(.secondary)
                        Button {
                            importingLesson = true
                        } label: { Label("选择课程 JSON", systemImage: "doc.text") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("课程 JSON 格式").font(.headline)
                        Text("""
                        {
                          "lessons": [
                            {
                              "n": 1,
                              "title": "урок 1",
                              "audio": "",
                              "segments": [
                                { "i": 1, "ru": "俄语句子", "zh": "中文",
                                  "start": 0.0, "end": 3.5, "audio": "" }
                              ]
                            }
                          ]
                        }
                        """)
                        .font(.caption2.monospaced())
                        .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("导入 / 下载资源")
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(isPresented: $importing, allowedContentTypes: [.json, .plainText, .commaSeparatedText, .tabSeparatedText, .item]) { result in
            switch result {
            case .success(let url):
                let ok = url.startAccessingSecurityScopedResource()
                defer { if ok { url.stopAccessingSecurityScopedResource() } }
                if let name = repo.importBook(from: url, name: nil) {
                    msg = "已导入词书「\(name)」，现有 \(repo.vocabBooks.count) 本"
                } else {
                    msg = "导入失败：文件为空或格式不符"
                }
            case .failure:
                msg = "导入失败"
            }
        }
        .fileImporter(isPresented: $importingZip, allowedContentTypes: [.zip]) { result in
            switch result {
            case .success(let url):
                let ok = url.startAccessingSecurityScopedResource()
                defer { if ok { url.stopAccessingSecurityScopedResource() } }
                let n = repo.importLessonZip(from: url)
                msg = n > 0 ? "已导入 \(n) 课，现有 \(repo.lessons.count) 课"
                            : "导入失败：包里没有找到清单 JSON"
            case .failure:
                msg = "导入失败"
            }
        }
        .fileImporter(isPresented: $importingLesson, allowedContentTypes: [.json, .plainText, .item]) { result in
            switch result {
            case .success(let url):
                let ok = url.startAccessingSecurityScopedResource()
                defer { if ok { url.stopAccessingSecurityScopedResource() } }
                let n = repo.importLessons(from: url)
                msg = n > 0 ? "已导入 \(n) 课，现有 \(repo.lessons.count) 课"
                            : "导入失败：不是有效的课程 JSON"
            case .failure:
                msg = "导入失败"
            }
        }
    }

    @State private var importing = false
    @State private var importingZip = false
    @State private var importingLesson = false
}
