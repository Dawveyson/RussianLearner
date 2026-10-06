import Foundation
import AVFoundation

/// 随身听播放器。
@MainActor
final class AudioPlayerController: NSObject, ObservableObject {
    static let shared = AudioPlayerController()

    @Published var isPlaying = false
    @Published var lessonNo = 0
    @Published var index = 0
    @Published var total = 0
    @Published var positionMs = 0
    @Published var durationMs = 0
    @Published private(set) var segments: [Segment] = []

    private var player: AVAudioPlayer?
    private var timer: Timer?
    private var lessonAudio = ""
    private var remoteCache: [URL: AVAudioPlayer] = [:]

    var currentText: String {
        guard index < segments.count else { return "" }
        let s = segments[index]
        return s.zh.isEmpty ? s.ru : s.zh
    }

    func build(lessons: [Lesson], lessonNo: Int, random: Bool) {
        stop()
        var picked: [Segment] = []
        var audio = ""
        if lessonNo == 0 {
            picked = lessons.flatMap { $0.segments }
            audio = lessons.first(where: { !$0.audio.isEmpty })?.audio ?? ""
        } else if let l = lessons.first(where: { $0.n == lessonNo }) {
            picked = l.segments
            audio = l.audio
        }
        if random { picked.shuffle() }
        segments = picked
        lessonAudio = audio
        total = picked.count
        index = 0
        self.lessonNo = lessonNo
    }

    func play() { start(at: index) }

    func toggle() {
        if isPlaying {
            player?.pause(); isPlaying = false; stopTimer()
        } else {
            play()
        }
    }

    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
        positionMs = 0
        durationMs = 0
        stopTimer()
    }

    func next() {
        guard !segments.isEmpty else { return }
        start(at: (index + 1) % segments.count)
    }

    func prev() {
        guard !segments.isEmpty else { return }
        start(at: (index - 1 + segments.count) % segments.count)
    }

    func seek(toMs ms: Int) {
        player?.currentTime = Double(ms) / 1000
        positionMs = ms
    }

    func seekSegment(_ i: Int) {
        guard i >= 0, i < segments.count else { return }
        start(at: i)
    }

    private func start(at i: Int) {
        guard i < segments.count else { return }
        index = i
        stopTimer()
        let s = segments[i]
        let path = s.audio.isEmpty ? lessonAudio : s.audio
        guard let url = resolve(path) else { return }
        if url.isFileURL {
            makePlayer(url, segment: s, useTimeline: s.audio.isEmpty)
        } else {
            playRemote(url, segment: s, useTimeline: s.audio.isEmpty)
        }
    }

    private func makePlayer(_ url: URL, segment: Segment, useTimeline: Bool) {
        do {
            try? AVAudioSession.sharedInstance().setActive(true)
            let p = try AVAudioPlayer(contentsOf: url)
            p.delegate = self
            player = p
            if useTimeline, segment.end > segment.start {
                p.currentTime = segment.start
                durationMs = Int((segment.end - segment.start) * 1000)
            } else {
                durationMs = Int(p.duration * 1000)
            }
            p.prepareToPlay()
            p.play()
            isPlaying = true
            startTimer()
        } catch {
            isPlaying = false
        }
    }

    private func playRemote(_ url: URL, segment: Segment, useTimeline: Bool) {
        if let cached = remoteCache[url] {
            cached.currentTime = useTimeline ? segment.start : 0
            player = cached
            durationMs = (useTimeline && segment.end > segment.start)
                ? Int((segment.end - segment.start) * 1000) : Int(cached.duration * 1000)
            cached.play()
            isPlaying = true
            startTimer()
            return
        }
        Task { @MainActor in
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  let p = try? AVAudioPlayer(data: data) else {
                self.isPlaying = false
                return
            }
            p.delegate = self
            self.remoteCache[url] = p
            self.player = p
            if useTimeline, segment.end > segment.start {
                p.currentTime = segment.start
                self.durationMs = Int((segment.end - segment.start) * 1000)
            } else {
                self.durationMs = Int(p.duration * 1000)
            }
            p.play()
            self.isPlaying = true
            self.startTimer()
        }
    }

    private func resolve(_ path: String) -> URL? {
        if path.isEmpty { return nil }
        if path.hasPrefix("http://") || path.hasPrefix("https://") { return URL(string: path) }
        if path.hasPrefix("file://") { return URL(string: path) }
        if path.hasPrefix("/") { return URL(fileURLWithPath: path) }
        return URL(fileURLWithPath: AppFiles.dir.appendingPathComponent(path).path)
    }

    private func startTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, let p = self.player else { return }
                self.positionMs = Int(p.currentTime * 1000)
                if self.durationMs <= 0 { self.durationMs = Int(p.duration * 1000) }
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}

extension AudioPlayerController: AVAudioPlayerDelegate {
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            if AudioPlayerController.shared.isPlaying {
                AudioPlayerController.shared.next()
            }
        }
    }
}
