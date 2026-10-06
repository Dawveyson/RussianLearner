import SwiftUI

// MARK: - 今日复习（看中文回忆俄语）

struct ReviewView: View {
    @EnvironmentObject var repo: AppRepository
    @Environment(\.dismiss) private var dismiss
    @State private var queue: [WordEntry] = []
    @State private var idx = 0
    @State private var revealed = false
    @State private var correctCount = 0

    var body: some View {
        VStack(spacing: 18) {
            if queue.isEmpty {
                EmptyHint("没有可复习的词，先导入词库吧")
                Button("返回") { dismiss() }.buttonStyle(GlassSecondaryButtonStyle())
            } else {
                header
                card
                Spacer()
            }
        }
        .padding(20)
        .navigationTitle("今日复习")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if queue.isEmpty { queue = repo.reviewQueue() } }
    }

    private var header: some View {
        HStack {
            Text("\(min(idx + 1, queue.count)) / \(queue.count)")
                .font(.subheadline.monospacedDigit())
                .foregroundStyle(.secondary)
            Spacer()
            Text("答对 \(correctCount)")
                .font(.subheadline).foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    private var card: some View {
        let e = queue[min(idx, queue.count - 1)]
        GlassCard {
            VStack(spacing: 14) {
                Text(e.zh).font(.largeTitle.bold()).multilineTextAlignment(.center)
                if revealed {
                    Text(e.ru).font(.title).foregroundStyle(.tint)
                    Text("已见过 \(repo.masteryOf(e.ru).seen) 次 · 正确 \(repo.masteryOf(e.ru).ok)")
                        .font(.caption).foregroundStyle(.secondary)
                }
                HStack(spacing: 10) {
                    Button {
                        Speaker.shared.say(e.ru)
                    } label: { Image(systemName: "speaker.wave.2.fill") }
                        .buttonStyle(GlassSecondaryButtonStyle())
                    if !revealed {
                        Button("显示答案") { revealed = true }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    } else {
                        Button("记住了") { answer(true) }
                            .buttonStyle(GlassPrimaryButtonStyle())
                        Button("没记住") { answer(false) }
                            .buttonStyle(GlassSecondaryButtonStyle())
                    }
                }
            }
        }
    }

    private func answer(_ ok: Bool) {
        let e = queue[idx]
        repo.recordAnswer(e.ru, correct: ok)
        if ok { correctCount += 1 }
        revealed = false
        if idx + 1 >= queue.count {
            queue = []
            idx = 0
        } else {
            idx += 1
        }
    }
}

// MARK: - 拼写（中文 → 拼俄语，用内置俄文键盘）

struct SpellingView: View {
    @EnvironmentObject var repo: AppRepository
    @Environment(\.dismiss) private var dismiss
    @State private var queue: [WordEntry] = []
    @State private var idx = 0
    @State private var input = ""
    @State private var checked = false
    @State private var wasCorrect = false
    @State private var right = 0
    @State private var total = 0

    var body: some View {
        VStack(spacing: 14) {
            if queue.isEmpty {
                EmptyHint("没有可拼写的词，先导入词库吧")
                Button("返回") { dismiss() }.buttonStyle(GlassSecondaryButtonStyle())
            } else {
                header
                GlassCard {
                    VStack(spacing: 12) {
                        Text(queue[idx].zh)
                            .font(.title.bold())
                            .multilineTextAlignment(.center)
                        Text(input.isEmpty ? "　" : input)
                            .font(.title2.monospaced())
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(.thinMaterial, in: .rect(cornerRadius: 10))
                        if checked {
                            Label(wasCorrect ? "正确" : "正确拼写：\(queue[idx].ru)",
                                  systemImage: wasCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .foregroundStyle(wasCorrect ? .green : .red)
                        }
                    }
                }
                if checked {
                    Button("下一个") { next() }.buttonStyle(GlassPrimaryButtonStyle())
                } else {
                    Button("检查") { check() }.buttonStyle(GlassPrimaryButtonStyle())
                        .disabled(input.isEmpty)
                }
                RuKeyboard(
                    onChar: { if !checked { input += $0 } },
                    onBackspace: { if !checked && !input.isEmpty { input.removeLast() } },
                    onSpace: { if !checked { input += " " } }
                )
            }
        }
        .padding(16)
        .navigationTitle("拼写")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if queue.isEmpty { start() } }
    }

    private var header: some View {
        HStack {
            Text("正确率 \(Prefs.spelling.correct)/\(Prefs.spelling.total)")
                .font(.caption).foregroundStyle(.secondary)
            Spacer()
            Text("本轮 \(right)/\(total)").font(.caption).foregroundStyle(.secondary)
        }
    }

    private func start() {
        queue = Array(repo.practiceBooks.flatMap { $0.entries }.shuffled().prefix(30))
        idx = 0
    }

    private func check() {
        let e = queue[idx]
        wasCorrect = norm(input) == norm(e.ru)
        checked = true
        right += wasCorrect ? 1 : 0
        total += 1
        Prefs.recordSpelling(correct: wasCorrect)
        repo.recordAnswer(e.ru, correct: wasCorrect)
    }

    private func next() {
        input = ""
        checked = false
        if idx + 1 >= queue.count {
            queue = []
            idx = 0
        } else {
            idx += 1
        }
    }
}
