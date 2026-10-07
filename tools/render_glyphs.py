# -*- coding: utf-8 -*-
"""把笔画数据渲染成 SVG，人工核对字形与笔顺。"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from glyph_strokes import UPPER, LOWER

def strokes_of(spec):
    """返回 [(pts, mode)]，mode: 'line' 尖角折线 / 'curve' 平滑 / 'loop' 闭合圆圈"""
    out = []
    for s in spec.split(";"):
        s = s.strip()
        if not s: continue
        mode = "line"
        if s[0] == "+":
            mode, s = "curve", s[1:]
        elif s[0] == "*":
            mode, s = "loop", s[1:]
        pts = []
        for p in s.split():
            x, y = p.split(",")
            pts.append((float(x), float(y)))
        if pts: out.append((pts, mode))
    return out

COLORS = ["#2f6fd0", "#d0452f", "#2f9d55", "#a345cf", "#d08a2f", "#2fa8a8", "#c0407a", "#5a7d2f"]

def smooth_path(pts, close=False):
    """把折线转成平滑二次贝塞尔路径（与 App 端 Path.quadraticBezier 逻辑一致）。"""
    if len(pts) < 2:
        return f"M {pts[0][0]:.2f},{pts[0][1]:.2f}" if pts else ""
    d = f"M {pts[0][0]:.2f},{pts[0][1]:.2f}"
    seq = list(pts[1:])
    if close:
        seq.append(pts[0])
    for i in range(len(seq) - 1):
        cur = seq[i]
        nxt = seq[i + 1]
        mx, my = (cur[0] + nxt[0]) / 2, (cur[1] + nxt[1]) / 2
        d += f" Q {cur[0]:.2f},{cur[1]:.2f} {mx:.2f},{my:.2f}"
    last = seq[-1]
    d += f" L {last[0]:.2f},{last[1]:.2f}"
    return d

def render(items, cell=130, pad=16, cols=7, title=""):
    rows = (len(items) + cols - 1) // cols
    top = 34 if title else 0
    W, H = cols * cell, rows * cell + top
    p = [f'<rect width="{W}" height="{H}" fill="#ffffff"/>']
    if title:
        p.append(f'<text x="12" y="22" font-size="16" fill="#222" font-family="Helvetica" '
                 f'font-weight="bold">{title}</text>')
    for idx, ch in enumerate(items):
        cx, cy = (idx % cols) * cell, (idx // cols) * cell + top
        p.append(f'<rect x="{cx+2}" y="{cy+2}" width="{cell-4}" height="{cell-4}" fill="none" stroke="#dddddd"/>')
        spec = UPPER.get(ch) or LOWER.get(ch)
        if not spec:
            p.append(f'<text x="{cx+cell/2}" y="{cy+cell/2+8}" font-size="22" fill="#c00" '
                     f'text-anchor="middle" font-family="Helvetica">缺</text>')
            continue
        w = cell - 2 * pad
        def pt(x, y): return (cx + pad + x * w, cy + pad + y * w)
        st = strokes_of(spec)
        # 底：完整浅色字形（描红用）
        for pts, mode in st:
            abs_pts = [pt(x, y) for x, y in pts]
            if mode == "line":
                ps = " ".join(f"{a:.1f},{b:.1f}" for a, b in abs_pts)
                d = "M " + ps.replace(" ", " L ")
            else:
                d = smooth_path(abs_pts, close=(mode == "loop"))
            p.append(f'<path d="{d}" fill="none" stroke="#eeeeee" stroke-width="9" '
                     f'stroke-linecap="round" stroke-linejoin="round"/>')
        # 笔顺：逐笔 + 编号
        for si, (pts, mode) in enumerate(st):
            abs_pts = [pt(x, y) for x, y in pts]
            if mode == "line":
                ps = " ".join(f"{a:.1f},{b:.1f}" for a, b in abs_pts)
                d = "M " + ps.replace(" ", " L ")
            else:
                d = smooth_path(abs_pts, close=(mode == "loop"))
            c = COLORS[si % len(COLORS)]
            p.append(f'<path d="{d}" fill="none" stroke="{c}" stroke-width="6" '
                     f'stroke-linecap="round" stroke-linejoin="round"/>')
            sx, sy = pt(pts[0][0], pts[0][1])
            p.append(f'<circle cx="{sx:.1f}" cy="{sy:.1f}" r="8" fill="{c}"/>')
            p.append(f'<text x="{sx:.1f}" y="{sy+4:.1f}" font-size="10" fill="#fff" text-anchor="middle" '
                     f'font-family="Helvetica" font-weight="bold">{si+1}</text>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">' + "".join(p) + '</svg>'

if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "upper"
    out = sys.argv[2] if len(sys.argv) > 2 else "/tmp/glyphs"
    if which == "upper":
        svg = render(list(UPPER.keys()), cols=7, title="Cyrillic UPPERCASE — stroke order")
    else:
        svg = render(list(LOWER.keys()), cols=7, title="Cyrillic lowercase — stroke order")
    path = out + ".svg"
    open(path, "w", encoding="utf-8").write(svg)
    print(path)
