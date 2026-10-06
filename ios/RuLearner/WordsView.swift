import SwiftUI

struct WordsView: View {
    @EnvironmentObject var repo: AppRepository
    @State private var query = ""
    @State private var activeBook = ""
    @State private var showImport = false

    private var results: [WordEntry] {
        if !query.trimmingCharacters(in: .whitespaces).isEmpty {
            return repo.search(query)
        }
        if !activeBook.isEmpty, let b = repo.lookupBooks.first(where: { $0.id == activeBook }) {
            return b.entries
        }
        return []
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 10) {
                    if repo.lookupBooks.isEmpty {
                        EmptyHint("还没有词库，去「个人 → 导入 / 下载资源」添加")
                    } else {
                        bookPicker
                        if query.isEmpty && activeBook.isEmpty {
                            ForEach(repo.lookupBooks) { b in
                                bookRow(b)
                            }
                        } else if results.isEmpty {
                            EmptyHint("没有匹配的词条")
                        } else {
                            ForEach(results) { e in
                                NavigationLink {
                                    WordDetailView(entry: e)
                                } label: {
                                    wordRow(e)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("单词")
            .searchable(text: $query, prompt: "搜索俄语或中文")
            .navigationDestination(isPresented: $showImport) { ImportView() }
        }
    }

    private var bookPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(title: "全部", selected: activeBook.isEmpty && query.isEmpty) {
                    activeBook = ""; query = ""
                }
                ForEach(repo.lookupBooks) { b in
                    chip(title: b.name, selected: activeBook == b.id) {
                        activeBook = b.id; query = ""
                    }
                }
            }
            .padding(.horizontal, 2)
        }
    }

    private func chip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .glassEffect(.regular.tint(selected ? .accentColor : .clear),
                              in: .capsule)
        }
        .buttonStyle(.plain)
    }

    private func bookRow(_ b: WordBook) -> some View {
        Button {
            activeBook = b.id
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(b.name).font(.headline)
                    Text("\(b.entries.count) 条词条\(b.hasAudio ? " · 含音频" : "")")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.secondary)
            }
            .padding(16)
        }
        .buttonStyle(.plain)
        .glassEffect(.regular, in: .rect(cornerRadius: 18))
    }

    private func wordRow(_ e: WordEntry) -> some View {
        let m = repo.masteryOf(e.ru)
        return HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(e.ru).font(.body.weight(.semibold))
                Text(e.zh).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
            if m.seen > 0 {
                Text("Lv\(m.lvl)").font(.caption2.monospacedDigit())
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(.thinMaterial, in: .capsule)
            }
            Button {
                Speaker.shared.say(e.ru)
            } label: { Image(systemName: "speaker.wave.2") }
                .buttonStyle(.plain)
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 16))
    }
}
