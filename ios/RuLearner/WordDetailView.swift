import SwiftUI

struct WordDetailView: View {
    @EnvironmentObject var repo: AppRepository
    let entry: WordEntry
    @State private var online: OnlineMeaning?
    @State private var searching = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 12) {
                        Text(entry.ru).font(.largeTitle.bold())
                        Text(entry.zh).font(.title3).foregroundStyle(.secondary)
                        Button {
                            Speaker.shared.say(entry.ru)
                        } label: { Label("朗读", systemImage: "speaker.wave.2.fill") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("掌握度").font(.headline)
                        let m = repo.masteryOf(entry.ru)
                        HStack {
                            StatTile(value: "\(m.lvl)", label: "等级")
                            StatTile(value: "\(m.seen)", label: "见过")
                            StatTile(value: "\(m.ok)", label: "答对")
                            StatTile(value: "\(m.bad)", label: "答错")
                        }
                    }
                }

                GlassCard {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("在线词典").font(.headline)
                            Spacer()
                            if searching { ProgressView() }
                        }
                        if let o = online {
                            Text(o.translation)
                            if let p = o.phonetic { Text(p).font(.caption).foregroundStyle(.secondary) }
                        } else {
                            Text("点击下方按钮联网查询（有道，国内可访问）")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        Button {
                            search()
                        } label: { Label("查询", systemImage: "magnifyingglass") }
                            .buttonStyle(GlassSecondaryButtonStyle())
                    }
                }

                if !entry.note.isEmpty {
                    GlassCard {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("备注").font(.headline)
                            Text(entry.note).foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(entry.ru)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func search() {
        searching = true
        Task {
            online = await OnlineDict.lookup(word: entry.ru,
                                            appKey: Prefs.apiKey,
                                            appSecret: Prefs.apiSecret)
            searching = false
        }
    }
}
