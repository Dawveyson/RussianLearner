import Foundation

// MARK: - 纯 Swift DEFLATE 解压（RFC 1951）
// iOS Foundation 不提供 zip 解压能力，而「zip 音频书导入」是核心功能，
// 因此这里实现一个最小可用的 inflate（结构参考 zlib 的 puff 算法）。

enum InflateError: Error { case badFormat }

private final class BitReader {
    private let data: [UInt8]
    private var pos = 0
    private var buf: UInt32 = 0
    private var cnt = 0

    init(_ d: Data) { data = [UInt8](d) }

    func bit() throws -> Int {
        if cnt == 0 {
            guard pos < data.count else { throw InflateError.badFormat }
            buf = UInt32(data[pos]); pos += 1; cnt = 8
        }
        let b = Int(buf & 1)
        buf >>= 1; cnt -= 1
        return b
    }

    func bits(_ n: Int) throws -> Int {
        var v = 0
        for i in 0..<n { v |= (try bit()) << i }
        return v
    }

    func align() { buf = 0; cnt = 0 }
    func skip(_ n: Int) { pos += n }
    var bytePos: Int { pos }
    var bytes: [UInt8] { data }
}

private struct Huffman {
    let counts: [Int]
    let symbols: [Int]

    init(lengths: [Int]) {
        var counts = [Int](repeating: 0, count: 16)
        for l in lengths where l > 0 { counts[l] += 1 }
        var offs = [Int](repeating: 0, count: 16)
        for i in 1..<16 { offs[i] = offs[i - 1] + counts[i - 1] }
        var syms = [Int](repeating: 0, count: lengths.filter { $0 > 0 }.count)
        for (sym, l) in lengths.enumerated() where l > 0 {
            syms[offs[l]] = sym
            offs[l] += 1
        }
        self.counts = counts
        self.symbols = syms
    }

    func decode(_ br: BitReader) throws -> Int {
        var code = 0, first = 0, index = 0
        for len in 1...15 {
            code |= try br.bit()
            let count = counts[len]
            if code - first < count { return symbols[index + (code - first)] }
            index += count
            first = (first + count) << 1
            code <<= 1
        }
        throw InflateError.badFormat
    }
}

enum Inflater {

    private static let lenBase = [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
                                  35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258]
    private static let lenExtra = [0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
                                   3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0]
    private static let distBase = [1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
                                   257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
                                   8193, 12289, 16385, 24577]
    private static let distExtra = [0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
                                    7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13]

    private static let clenOrder = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15]

    /// 原始 DEFLATE（RFC 1951）解压。
    static func raw(_ input: Data) throws -> Data {
        let br = BitReader(input)
        var out = [UInt8]()
        out.reserveCapacity(input.count * 4)

        var fixedLit: Huffman?
        var fixedDist: Huffman?

        while true {
            let last = try br.bit()
            let type = try br.bits(2)

            switch type {
            case 0: // stored
                br.align()
                let p = br.bytePos
                let d = br.bytes
                guard p + 4 <= d.count else { throw InflateError.badFormat }
                let len = Int(d[p]) | (Int(d[p + 1]) << 8)
                guard p + 4 + len <= d.count else { throw InflateError.badFormat }
                out.append(contentsOf: d[(p + 4)..<(p + 4 + len)])
                br.skip(len + 4)

            case 1: // fixed huffman
                if fixedLit == nil {
                    var lit = [Int](repeating: 0, count: 288)
                    for i in 0..<144 { lit[i] = 8 }
                    for i in 144..<256 { lit[i] = 9 }
                    for i in 256..<280 { lit[i] = 7 }
                    for i in 280..<288 { lit[i] = 8 }
                    fixedLit = Huffman(lengths: lit)
                    fixedDist = Huffman(lengths: [Int](repeating: 5, count: 30))
                }
                try block(br, out: &out, lit: fixedLit!, dist: fixedDist!)

            case 2: // dynamic huffman
                let hlit = try br.bits(5) + 257
                let hdist = try br.bits(5) + 1
                let hclen = try br.bits(4) + 4
                var clen = [Int](repeating: 0, count: 19)
                for i in 0..<hclen { clen[clenOrder[i]] = try br.bits(3) }
                let clHuff = Huffman(lengths: clen)
                var lengths = [Int]()
                lengths.reserveCapacity(hlit + hdist)
                while lengths.count < hlit + hdist {
                    let sym = try clHuff.decode(br)
                    switch sym {
                    case 0...15:
                        lengths.append(sym)
                    case 16:
                        let rep = try br.bits(2) + 3
                        guard let last = lengths.last else { throw InflateError.badFormat }
                        lengths.append(contentsOf: [Int](repeating: last, count: rep))
                    case 17:
                        lengths.append(contentsOf: [Int](repeating: 0, count: try br.bits(3) + 3))
                    case 18:
                        lengths.append(contentsOf: [Int](repeating: 0, count: try br.bits(7) + 11))
                    default:
                        throw InflateError.badFormat
                    }
                }
                guard lengths.count >= hlit + hdist else { throw InflateError.badFormat }
                let lit = Huffman(lengths: Array(lengths[0..<hlit]))
                let dist = Huffman(lengths: Array(lengths[hlit..<(hlit + hdist)]))
                try block(br, out: &out, lit: lit, dist: dist)

            default:
                throw InflateError.badFormat
            }

            if last == 1 { break }
        }
        return Data(out)
    }

    private static func block(_ br: BitReader, out: inout [UInt8], lit: Huffman, dist: Huffman) throws {
        while true {
            let sym = try lit.decode(br)
            if sym < 256 {
                out.append(UInt8(sym))
            } else if sym == 256 {
                return
            } else {
                let li = sym - 257
                guard li < lenBase.count else { throw InflateError.badFormat }
                let length = lenBase[li] + (try br.bits(lenExtra[li]))
                let ds = try dist.decode(br)
                guard ds < distBase.count else { throw InflateError.badFormat }
                let distance = distBase[ds] + (try br.bits(distExtra[ds]))
                guard distance <= out.count, distance > 0 else { throw InflateError.badFormat }
                var src = out.count - distance
                for _ in 0..<length {
                    out.append(out[src])
                    src += 1
                }
            }
        }
    }
}

// MARK: - 最小 zip 读取器

struct ZipEntry {
    let path: String
    let data: Data
}

enum Zip {
    fileprivate static func u16(_ d: [UInt8], _ off: Int) -> Int {
        guard off + 2 <= d.count else { return 0 }
        return Int(d[off]) | (Int(d[off + 1]) << 8)
    }

    fileprivate static func u32(_ d: [UInt8], _ off: Int) -> Int {
        guard off + 4 <= d.count else { return 0 }
        return Int(d[off]) | (Int(d[off + 1]) << 8) | (Int(d[off + 2]) << 16) | (Int(d[off + 3]) << 24)
    }

    /// 读取 zip 内所有文件条目。
    static func entries(of url: URL) -> [ZipEntry] {
        guard let data = try? Data(contentsOf: url) else { return [] }
        let d = [UInt8](data)

        // 定位 EOCD（签名 0x06054b50）
        var eocd = -1
        let lower = max(0, d.count - 66_000)
        var i = d.count - 22
        while i >= lower {
            if u32(d, i) == 0x0605_4b50 { eocd = i; break }
            i -= 1
        }
        guard eocd >= 0 else { return [] }

        let count = u16(d, eocd + 10)
        var offset = u32(d, eocd + 16)

        var out: [ZipEntry] = []
        for _ in 0..<count {
            guard offset + 46 <= d.count, u32(d, offset) == 0x0201_4b50 else { break }
            let method = u16(d, offset + 10)
            let compSize = u32(d, offset + 20)
            let nameLen = u16(d, offset + 28)
            let extraLen = u16(d, offset + 30)
            let commentLen = u16(d, offset + 32)
            let localOff = u32(d, offset + 42)
            let nameData = Data(d[(offset + 46)..<(offset + 46 + nameLen)])
            let name = String(data: nameData, encoding: .utf8)
                ?? String(data: nameData, encoding: .isoLatin1)
                ?? ""
            offset += 46 + nameLen + extraLen + commentLen

            if name.isEmpty || name.hasSuffix("/") { continue }

            guard localOff + 30 <= d.count, u32(d, localOff) == 0x0403_4b50 else { continue }
            let lNameLen = u16(d, localOff + 26)
            let lExtraLen = u16(d, localOff + 28)
            let start = localOff + 30 + lNameLen + lExtraLen
            guard start + compSize <= d.count else { continue }
            let payload = Data(d[start..<(start + compSize)])

            var content = Data()
            if method == 0 {
                content = payload
            } else if method == 8 {
                guard let inflated = try? Inflater.raw(payload) else { continue }
                content = inflated
            } else {
                continue
            }
            out.append(ZipEntry(path: name, data: content))
        }
        return out
    }
}
