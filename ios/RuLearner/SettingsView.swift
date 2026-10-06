import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject var repo: AppRepository
    @StateObject private var remote = RemoteConfigStore.shared

    @State private var goalText = ""
    @State private var goalMsg = ""
    @State private var key = ""
    @State private var secret = ""
    @State private var keyMsg = ""
    @State private var preferNet = true
    @State private var backupMsg = ""
    @State private var confirmReset = false
    @State private var showUpdate = false
    @State private var showAnnounce = false
    @State private var exporting = false
    @State private var importingBackup = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                goalCard
                dictCard
                audioCard
                backupCard
                dataCard
                aboutCard
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            goalText = String(Prefs.dailyGoal)
            key = Prefs.apiKey
            secret = Prefs.apiSecret
            preferNet = Prefs.preferNetworkTTS
        }
        .confirmationDialog("清除全部掌握度？", isPresented: $confirmReset, titleVisibility: .visible) {
            Button("清除", role: .destructive) { repo.resetProgress() }
            Button("取消", role: .cancel) {}
        } message: {
            Text("所有单词的等级与复习时间会被清空，词库本身不受影响。")
        }
        .alert("公告", isPresented: $showAnnounce) {
            Button("关闭") { showAnnounce = false }
        } message: {
            let anns = remote.config?.announcements ?? []
            if anns.isEmpty {
                Text("暂无可显示公告。")
            } else {
                Text(anns.map { "【\($0.title)】\($0.body)" }.joined(separator: "\n\n"))
            }
        }
        .alert("检查更新", isPresented: $showUpdate) {
            Button("关闭") { showUpdate = false }
            if let u = URL(string: remote.config?.apkUrl ?? "") {
                Button("去下载") { UIApplication.shared.open(u) }
            }
        } message: {
            let c = remote.config
            if remote.hasUpdate {
                Text("最新：v\(c?.latestVersionName ?? "")（\(c?.latestVersionCode ?? 0)）\n当前：v\(remote.currentVersionName)（\(remote.currentVersionCode)）")
            } else {
                Text("当前已是最新版本 v\(remote.currentVersionName)。")
            }
        }
        .fileExporter(isPresented: $exporting,
                      document: BackupDocument(text: BackupStore.exportJSON()),
                      contentType: .json,
                      defaultFilename: BackupStore.suggestedFileName()) { result in
            switch result {
            case .success(let url):
                backupMsg = "已导出：\(url.lastPathComponent)。放到桌面后运行 tools/backup_progress.sh 可上传 GitHub。"
            case .failure:
                backupMsg = "导出失败"
            }
        }
        .fileImporter(isPresented: $importingBackup,
                      allowedContentTypes: [.json, .plainText, .item]) { result in
            switch result {
            case .success(let url):
                let ok = url.startAccessingSecurityScopedResource()
                defer { if ok { url.stopAccessingSecurityScopedResource() } }
                if let text = try? String(contentsOf: url, encoding: .utf8) {
                    backupMsg = BackupStore.importJSON(text)
                        ? "已导入学习进度，掌握度与设置已恢复。"
                        : "导入失败：文件格式不对或已损坏。"
                    repo.reload()
                } else {
                    backupMsg = "导入失败：无法读取文件"
                }
            case .failure:
                backupMsg = "导入失败"
            }
        }
    }

    private var goalCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("每日目标").font(.headline)
                Text("首页进度环按这个数字计算今日学习进度（5–200）。")
                    .font(.caption).foregroundStyle(.secondary)
                TextField("每天学多少个词", text: $goalText)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(.thinMaterial, in: .rect(cornerRadius: 10))
                Button {
                    let v = Int(goalText) ?? 20
                    Prefs.dailyGoal = v
                    repo.refreshStats()
                    goalMsg = "已保存：每天 \(v) 个"
                } label: { Text("保存") }
                    .buttonStyle(GlassPrimaryButtonStyle())
                if !goalMsg.isEmpty {
                    Text(goalMsg).font(.caption).foregroundStyle(.tint)
                }
            }
        }
    }

    private var dictCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("在线词典（有道智云 · 国内可访问）").font(.headline)
                Text("不填也能用：内置演示接口。填入免费申请的 appKey / appSecret 后更稳定、额度更高。")
                    .font(.caption).foregroundStyle(.secondary)
                TextField("appKey", text: $key)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                    .textFieldStyle(.plain)
                    .padding(12).background(.thinMaterial, in: .rect(cornerRadius: 10))
                SecureField("appSecret", text: $secret)
                    .textFieldStyle(.plain)
                    .padding(12).background(.thinMaterial, in: .rect(cornerRadius: 10))
                Button {
                    Prefs.apiKey = key
                    Prefs.apiSecret = secret
                    keyMsg = "已保存"
                } label: { Text("保存密钥") }
                    .buttonStyle(GlassPrimaryButtonStyle())
                if !keyMsg.isEmpty {
                    Text(keyMsg).font(.caption).foregroundStyle(.tint)
                }
            }
        }
    }

    private var audioCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("发音").font(.headline)
                Toggle("优先使用网络发音（有道）", isOn: $preferNet)
                    .onChange(of: preferNet) { _, new in Prefs.preferNetworkTTS = new }
                Text("系统未装俄语语音包时会自动改用网络发音。")
                    .font(.caption).foregroundStyle(.secondary)
                Button {
                    Speaker.shared.say("Проверка произношения")
                } label: { Label("试听俄语发音", systemImage: "speaker.wave.2.fill") }
                    .buttonStyle(GlassSecondaryButtonStyle())
            }
        }
    }

    private var backupCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("备份与更新").font(.headline)
                Text("导出会把「掌握度 + 全部设置/统计」打包成一个 JSON；导入可恢复。")
                    .font(.caption).foregroundStyle(.secondary)
                Button { exporting = true } label: {
                    Label("导出学习进度", systemImage: "square.and.arrow.up")
                }
                .buttonStyle(GlassPrimaryButtonStyle())
                Button { importingBackup = true } label: {
                    Label("导入学习进度", systemImage: "square.and.arrow.down")
                }
                .buttonStyle(GlassSecondaryButtonStyle())
                HStack(spacing: 10) {
                    Button {
                        Task { await remote.refresh(); showUpdate = true }
                    } label: { Text("检查更新") }
                        .buttonStyle(GlassSecondaryButtonStyle())
                    Button {
                        Task { await remote.refresh() }
                        showAnnounce = true
                    } label: { Text("查看公告") }
                        .buttonStyle(GlassSecondaryButtonStyle())
                }
                if !backupMsg.isEmpty {
                    Text(backupMsg).font(.caption).foregroundStyle(.tint)
                }
            }
        }
    }

    private var dataCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("学习数据").font(.headline)
                Text("已掌握 \(repo.stats.mastered) 个词 · 连续 \(repo.stats.streak) 天 · 今日已学 \(repo.stats.learnedToday)")
                    .font(.caption).foregroundStyle(.secondary)
                Button(role: .destructive) { confirmReset = true } label: {
                    Label("清除全部掌握度记录", systemImage: "trash")
                }
                .buttonStyle(GlassSecondaryButtonStyle())
            }
        }
    }

    private var aboutCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 6) {
                Text("关于").font(.headline)
                Text("RuLearner v\(remote.currentVersionName) (\(remote.currentVersionCode))")
                    .font(.caption).foregroundStyle(.secondary)
                Text("原生 SwiftUI · Liquid Glass")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

/// 导出的备份文档。
struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    var text: String

    init(text: String) { self.text = text }

    init(configuration: ReadConfiguration) throws {
        text = configuration.file.regularFileContents
            .flatMap { String(data: $0, encoding: .utf8) } ?? "{}"
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
