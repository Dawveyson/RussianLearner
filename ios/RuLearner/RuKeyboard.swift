import SwiftUI

/// 内置俄文（ЙЦУКЕН）软键盘，避免切换系统输入法。
struct RuKeyboard: View {
    var onChar: (String) -> Void
    var onBackspace: () -> Void
    var onSpace: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            ForEach(RU_KEYBOARD_ROWS.indices, id: \.self) { r in
                HStack(spacing: 4) {
                    ForEach(RU_KEYBOARD_ROWS[r], id: \.self) { ch in
                        Button {
                            onChar(ch)
                        } label: {
                            Text(ch)
                                .font(.title3)
                                .frame(maxWidth: .infinity, minHeight: 44)
                        }
                        .buttonStyle(.plain)
                        .background(.thinMaterial, in: .rect(cornerRadius: 8))
                    }
                }
            }
            HStack(spacing: 6) {
                Button("空格") { onSpace() }
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .background(.thinMaterial, in: .rect(cornerRadius: 8))
                Button {
                    onBackspace()
                } label: {
                    Image(systemName: "delete.left").frame(width: 52, height: 44)
                }
                .buttonStyle(.plain)
                .background(.thinMaterial, in: .rect(cornerRadius: 8))
            }
        }
        .padding(8)
        .background(.ultraThinMaterial, in: .rect(cornerRadius: 20))
    }
}
