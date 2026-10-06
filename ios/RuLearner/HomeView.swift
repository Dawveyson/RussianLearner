import SwiftUI

struct HomeView: View {
    @EnvironmentObject var repo: AppRepository
    @State private var showSpelling = false
    @State private var showQuiz = false
    @State private var showReview = false

    private var goal: Int { max(repo.stats.dailyGoal, 1) }
    private var ratio: Double {
        min(Double(repo.stats.learnedToday) / Double(goal), 1)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    progressCard
                    statsCard
                    practiceCard
                    dailyWordCard
                }
                .padding(16)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("RuLearner")
            .navigationDestination(isPresented: $showSpelling) { SpellingView() }
            .navigationDestination(isPresented: $showQuiz) { QuizView() }
            .navigationDestination(isPresented: $showReview) { ReviewView() }
        }
    }

    private var progressCard: some View {
        GlassCard {
            HStack(spacing: 20) {
                ZStack {
                    Circle().stroke(.secondary.opacity(0.18), lineWidth: 12)
                    Circle()
                        .trim(from: 0, to: ratio)
                        .stroke(.tint, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .animation(.smooth, value: ratio)
                    VStack(spacing: 0) {
                        Text("\(repo.stats.learnedToday)")
                            .font(.title.bold())
                            .monospacedDigit()
                        Text("/ \(goal)").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .frame(width: 108, height: 108)

                VStack(alignment: .leading, spacing: 6) {
                    Text("今日学习进度").font(.headline)
                    Text("已掌握 \(repo.stats.mastered) 个词 · 待复习 \(repo.stats.dueToday) 个")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("连续打卡 \(repo.stats.streak) 天")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
            }
        }
    }

    private var statsCard: some View {
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

    private var practiceCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("开始练习").font(.headline)
                if repo.practiceBooks.isEmpty {
                    EmptyHint("先在「个人 → 导入 / 下载资源」添加词库")
                } else {
                    Button { showReview = true } label: { Label("今日复习", systemImage: "clock.arrow.circlepath") }
                        .buttonStyle(GlassPrimaryButtonStyle())
                    HStack(spacing: 10) {
                        Button { showSpelling = true } label: { Label("拼写", systemImage: "keyboard") }
                            .buttonStyle(GlassSecondaryButtonStyle())
                        Button { showQuiz = true } label: { Label("测验", systemImage: "checkmark.circle") }
                            .buttonStyle(GlassSecondaryButtonStyle())
                    }
                }
            }
        }
    }

    private var dailyWordCard: some View {
        GlassCard {
            let w = repo.dailyWord()
            return VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("每日一词").font(.headline)
                    Spacer()
                    Button {
                        Prefs.bumpWordOffset()
                    } label: { Image(systemName: "arrow.triangle.2.circlepath") }
                        .buttonStyle(.plain)
                }
                Text(w.ru).font(.title2.bold())
                Text(w.zh).foregroundStyle(.secondary)
                Button {
                    Speaker.shared.say(w.ru)
                } label: { Label("朗读", systemImage: "speaker.wave.2.fill") }
                    .buttonStyle(GlassSecondaryButtonStyle())
            }
        }
    }
}
