import SwiftUI

/// 随身听卡片：播放课文音频，支持整课音轨时间轴定位与逐句跳转。
struct PocketPlayerCard: View {
    @EnvironmentObject var repo: AppRepository
    @StateObject private var player = AudioPlayerController.shared
    @State private var lessonChoice = 0
    @State private var random = false
    @State private var showLines = false

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("随身听").font(.headline)
                if repo.lessons.isEmpty {
                    EmptyHint("先导入教材才能使用随身听")
                } else {
                    Text(player.currentText.isEmpty ? "未在播放" : player.currentText)
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Text("第 \(min(player.index + 1, max(player.total, 1))) / \(player.total) 段")
                        .font(.caption).foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    HStack {
                        Button { player.prev() } label: { Image(systemName: "backward.fill") }
                            .buttonStyle(.plain).frame(maxWidth: .infinity)
                        Button { player.toggle() } label: {
                            Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                .font(.title)
                        }
                        .buttonStyle(.plain).frame(maxWidth: .infinity)
                        Button { player.next() } label: { Image(systemName: "forward.fill") }
                            .buttonStyle(.plain).frame(maxWidth: .infinity)
                    }

                    if player.durationMs > 0 {
                        Slider(value: Binding(
                            get: { Double(player.positionMs) },
                            set: { player.seek(toMs: Int($0)) }
                        ), in: 0...Double(max(player.durationMs, 1)))
                        Text("\(fmt(player.positionMs)) / \(fmt(player.durationMs))")
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                    }

                    HStack(spacing: 10) {
                        Button {
                            player.build(lessons: repo.lessons, lessonNo: lessonChoice, random: random)
                            player.play()
                        } label: { Label("开始播放", systemImage: "headphones") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                        Button { player.stop() } label: { Label("停止", systemImage: "stop.fill") }
                            .buttonStyle(GlassSecondaryButtonStyle())
                    }

                    HStack {
                        Button(random ? "顺序" : "随机") { random.toggle() }
                            .buttonStyle(.plain).font(.subheadline)
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(.thinMaterial, in: .capsule)
                        Text(lessonChoice == 0 ? "全部课程" : "第 \(lessonChoice) 课")
                            .font(.subheadline).foregroundStyle(.secondary)
                        Spacer()
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip(0, "全部")
                            ForEach(repo.lessons) { l in chip(l.n, "урок \(l.n)") }
                        }
                    }

                    Button { showLines.toggle() } label: {
                        Text(showLines ? "隐藏逐句" : "逐句列表")
                    }
                    .buttonStyle(.plain).font(.subheadline)

                    if showLines {
                        VStack(spacing: 0) {
                            ForEach(player.segments) { seg in
                                let row = player.segments.firstIndex { $0.id == seg.id } ?? 0
                                Button { player.seekSegment(row) } label: {
                                    HStack {
                                        Text(seg.ru)
                                            .foregroundStyle(row == player.index ? Color.accentColor : Color.primary)
                                        Spacer()
                                        Text(seg.zh).font(.caption)
                                            .foregroundStyle(.secondary).lineLimit(1)
                                    }
                                    .font(.subheadline)
                                    .padding(.vertical, 8)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }

    private func chip(_ n: Int, _ title: String) -> some View {
        Button { lessonChoice = n } label: {
            Text(title)
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 12).padding(.vertical, 7)
                .glassEffect(.regular.tint(lessonChoice == n ? .accentColor : .clear), in: .capsule)
        }
        .buttonStyle(.plain)
    }

    private func fmt(_ ms: Int) -> String {
        let s = max(0, ms) / 1000
        return "\(s / 60):" + String(format: "%02d", s % 60)
    }
}

struct LessonView: View {
    @EnvironmentObject var repo: AppRepository
    let n: Int

    private var lesson: Lesson? { repo.lessons.first { $0.n == n } }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if let l = lesson {
                    ForEach(l.segments) { seg in
                        GlassCard {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(seg.ru).font(.body.weight(.semibold))
                                Text(seg.zh).font(.subheadline).foregroundStyle(.secondary)
                                HStack {
                                    Text("第 \(seg.i) 段").font(.caption2)
                                        .foregroundStyle(.secondary)
                                    Spacer()
                                    Button {
                                        if seg.audio.isEmpty, l.audio.isEmpty {
                                            Speaker.shared.say(seg.ru)
                                        } else {
                                            AudioPlayerController.shared.build(
                                                lessons: repo.lessons, lessonNo: n, random: false)
                                            AudioPlayerController.shared.seekSegment(seg.i - 1)
                                        }
                                    } label: { Label("播放", systemImage: "play.circle") }
                                        .buttonStyle(.plain).font(.caption)
                                }
                            }
                        }
                    }
                } else {
                    EmptyHint("课程不存在")
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("урок \(n)")
        .navigationBarTitleDisplayMode(.inline)
    }
}
