import SwiftUI

struct AlphabetView: View {
    private let cols = Array(repeating: GridItem(.flexible(), spacing: 10), count: 3)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    GlassCard {
                        Text("西里尔字母表（33 个字母）。点字母可朗读字母名与例词。")
                            .font(.subheadline).foregroundStyle(.secondary)
                    }
                    LazyVGrid(columns: cols, spacing: 10) {
                        ForEach(CYRILLIC_ALPHABET) { l in
                            NavigationLink {
                                LetterDetailView(letter: l)
                            } label: {
                                VStack(spacing: 2) {
                                    Text(l.upper).font(.title2.bold())
                                    Text(l.lower).font(.subheadline).foregroundStyle(.secondary)
                                }
                                .frame(maxWidth: .infinity, minHeight: 68)
                            }
                            .buttonStyle(.plain)
                            .glassEffect(.regular, in: .rect(cornerRadius: 16))
                        }
                    }
                }
                .padding(16)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("字母")
        }
    }
}

struct LetterDetailView: View {
    let letter: LetterInfo

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                GlassCard {
                    VStack(spacing: 10) {
                        HStack(spacing: 24) {
                            Text(letter.upper).font(.system(size: 64, weight: .bold))
                            Text(letter.lower).font(.system(size: 64, weight: .bold))
                                .foregroundStyle(.secondary)
                        }
                        Text(letter.sound).font(.title3).foregroundStyle(.tint)
                        Button {
                            Speaker.shared.say(letter.upper)
                        } label: { Label("朗读字母", systemImage: "speaker.wave.2.fill") }
                            .buttonStyle(GlassPrimaryButtonStyle())
                    }
                }
                GlassCard {
                    VStack(spacing: 8) {
                        Text("例词").font(.headline)
                        Text(letter.sampleRu).font(.title2)
                        Text(letter.sampleZh).foregroundStyle(.secondary)
                        Button {
                            Speaker.shared.say(letter.sampleRu)
                        } label: { Label("朗读例词", systemImage: "speaker.wave.2.fill") }
                            .buttonStyle(GlassSecondaryButtonStyle())
                    }
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(letter.upper)
        .navigationBarTitleDisplayMode(.inline)
    }
}
