import SwiftUI

struct MeView: View {
    @EnvironmentObject var repo: AppRepository
    @State private var showImport = false
    @State private var showSettings = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    overview
                    PocketPlayerCard()
                    manageCard
                    booksSection
                    lessonsSection
                    Text("Made by Davey With Heart💗 · VibeCoding")
                        .font(.caption).foregroundStyle(.secondary)
                        .padding(.top, 8)
                }
                .padding(16)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("个人")
            .navigationDestination(isPresented: $showImport) { ImportView() }
            .navigationDestination(isPresented: $showSettings) { SettingsView() }
            .navigationDestination(for: Int.self) { n in LessonView(n: n) }
        }
    }

    private var overview: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("学习概览").font(.headline)
                HStack {
                    StatTile(value: "\(repo.stats.totalWords)", label: "总词条")
                    StatTile(value: "\(repo.stats.mastered)", label: "已掌握")
                    StatTile(value: "\(repo.stats.streak)", label: "连续天数")
                    StatTile(value: "\(repo.stats.learnedToday)", label: "今日已学")
                }
            }
        }
    }

    private var manageCard: some View {
        GlassCard {
            VStack(spacing: 10) {
                navRow(title: "导入 / 下载资源",
                       sub: "词库 \(repo.vocabBooks.count) 本 · 教材 \(repo.lessons.count) 课",
                       icon: "square.and.arrow.down") { showImport = true }
                Divider()
                navRow(title: "设置", sub: "目标 · 词典 · 备份 · 更新",
                       icon: "gearshape") { showSettings = true }
            }
        }
    }

    private func navRow(title: String, sub: String, icon: String,
                        action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon).foregroundStyle(.tint)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.headline).foregroundStyle(.primary)
                    Text(sub).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.secondary)
            }
            .padding(.vertical, 6)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var booksSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle("我的词库（\(repo.vocabBooks.count)）")
            if repo.vocabBooks.isEmpty {
                EmptyHint("还没有词库，去「导入 / 下载资源」添加")
            } else {
                ForEach(repo.vocabBooks) { b in
                    GlassCard {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(b.name).font(.subheadline.weight(.semibold))
                                Text("\(b.entries.count) 条词条").font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button(role: .destructive) { repo.deleteBook(id: b.id) } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var lessonsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle("我的教材（\(repo.lessons.count)）")
            if repo.lessons.isEmpty {
                EmptyHint("还没有教材")
            } else {
                ForEach(repo.lessons) { l in
                    GlassCard {
                        HStack {
                            NavigationLink(value: l.n) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("урок \(l.n) · \(l.title)")
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(.primary)
                                    Text("\(l.segments.count) 段\(l.audio.isEmpty ? "" : " · 整课音轨")")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            .buttonStyle(.plain)
                            Spacer()
                            Button(role: .destructive) { repo.deleteLesson(n: l.n) } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }
}
