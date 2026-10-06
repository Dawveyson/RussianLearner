import SwiftUI

struct QuizView: View {
    @EnvironmentObject var repo: AppRepository
    @Environment(\.dismiss) private var dismiss
    @State private var questions: [Question] = []
    @State private var idx = 0
    @State private var picked: Int? = nil
    @State private var score = 0
    @State private var finished = false

    var body: some View {
        VStack(spacing: 16) {
            if questions.isEmpty && !finished {
                EmptyHint("没有可练习的词，先导入词库吧")
                Button("返回") { dismiss() }.buttonStyle(GlassSecondaryButtonStyle())
            } else if finished {
                result
            } else {
                quiz
            }
        }
        .padding(16)
        .navigationTitle("测验")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if questions.isEmpty { start() } }
    }

    private var quiz: some View {
        let q = questions[idx]
        return VStack(spacing: 14) {
            HStack {
                Text("\(idx + 1) / \(questions.count)").font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
                Spacer()
                Text("得分 \(score)").font(.subheadline).foregroundStyle(.secondary)
            }

            GlassCard {
                VStack(spacing: 8) {
                    Text(q.mode.rawValue).font(.caption).foregroundStyle(.secondary)
                    Text(q.prompt).font(.largeTitle.bold()).multilineTextAlignment(.center)
                }
            }

            if q.mode == .spell {
                GlassCard {
                    Text("用拼写练习完成这道题")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
            } else {
                ForEach(Array(q.options.indices), id: \.self) { i in
                    let opt = q.options[i]
                    Button {
                        pick(i)
                    } label: {
                        HStack {
                            Text(opt).font(.body).multilineTextAlignment(.leading)
                            Spacer()
                            if let p = picked {
                                if i == q.answerIndex {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                } else if i == p {
                                    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                                }
                            }
                        }
                        .padding(14)
                    }
                    .buttonStyle(.plain)
                    .background(.thinMaterial, in: .rect(cornerRadius: 16))
                    .disabled(picked != nil)
                }
            }

            if picked != nil {
                Button("下一题") { next() }.buttonStyle(GlassPrimaryButtonStyle())
            }
        }
    }

    private var result: some View {
        VStack(spacing: 18) {
            Text(score >= questions.count / 2 ? "闯关完成！最终得分 \(score)。" : "闯关失败，最终得分 \(score)。")
                .font(.title2.bold())
                .foregroundStyle(score >= questions.count / 2 ? .green : .red)
            GlassCard {
                VStack(spacing: 6) {
                    StatTile(value: "\(score)", label: "本轮得分")
                    Text("历史最佳 \(Prefs.quizBest)")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }
            Button("再来一轮") {
                questions = []
                finished = false
                start()
            }
            .buttonStyle(GlassPrimaryButtonStyle())
            Button("返回") { dismiss() }.buttonStyle(GlassSecondaryButtonStyle())
        }
    }

    private func start() {
        questions = buildQuestions(books: repo.practiceBooks, count: 10)
        idx = 0
        score = 0
        picked = nil
    }

    private func pick(_ i: Int) {
        guard picked == nil else { return }
        picked = i
        let q = questions[idx]
        if i == q.answerIndex {
            score += 1
            repo.recordAnswer(q.entry.ru, correct: true)
        } else {
            repo.recordAnswer(q.entry.ru, correct: false)
        }
    }

    private func next() {
        if idx + 1 >= questions.count {
            Prefs.setQuizBest(score)
            repo.refreshStats()
            finished = true
        } else {
            idx += 1
            picked = nil
        }
    }
}
