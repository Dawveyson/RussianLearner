import Foundation
import AVFoundation

/// 发音：优先系统俄语语音，未安装时回落到有道网络发音。
@MainActor
final class Speaker: ObservableObject {
    static let shared = Speaker()

    private let synth = AVSpeechSynthesizer()
    private var onlinePlayer: AVAudioPlayer?

    private init() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    /// 系统是否装有俄语语音。
    var hasRussianVoice: Bool { AVSpeechSynthesisVoice(language: "ru-RU") != nil }

    /// 朗读俄语。online=true 时强制走网络发音。
    func say(_ text: String, online: Bool = false) {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }

        if online || !hasRussianVoice {
            playOnline(clean)
            return
        }
        if let v = AVSpeechSynthesisVoice(language: "ru-RU") {
            synth.stopSpeaking(at: .immediate)
            let u = AVSpeechUtterance(string: clean)
            u.voice = v
            u.rate = AVSpeechUtteranceDefaultSpeechRate
            synth.speak(u)
        } else {
            playOnline(clean)
        }
    }

    /// 有道 dictvoice 网络发音（免密钥）。俄语必须 le=ru。
    private func playOnline(_ word: String) {
        guard var c = URLComponents(string: "https://dict.youdao.com/dictvoice") else { return }
        c.queryItems = [
            URLQueryItem(name: "audio", value: word),
            URLQueryItem(name: "le", value: "ru")
        ]
        guard let url = c.url else { return }
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard let data, !data.isEmpty else { return }
            Task { @MainActor in
                guard let p = try? AVAudioPlayer(data: data) else { return }
                self?.onlinePlayer?.stop()
                self?.onlinePlayer = p
                p.play()
            }
        }.resume()
    }

    func stop() {
        synth.stopSpeaking(at: .immediate)
        onlinePlayer?.stop()
    }
}
