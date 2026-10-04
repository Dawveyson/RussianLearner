#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
根据静音检测得到的 /tmp/seg_N.json（每课的精确语音段边界），
生成 App 可用的教材 ZIP：audio/урок N.mp3 + manifest.json。

时间戳来自音频本身（afconvert 解码 + 纯 Python 能量静音检测），精确。
文本稿：
  - урок 1 按教材已知俄语字母表循环标注（发音课内容即字母）；
  - урок 2 按已知音节表循环标注；
  - урок 3–11 暂无文本稿，用「урок N·序号」占位（时间戳仍精确），
    之后用户给文本稿可替换 ru 字段（build_textbook_zip.py 支持显式文本）。

用法:
    python3 build_manifest.py --out textbook.zip \
        --audio-dir "/Users/davey/Downloads/rar_extract/大学通用俄语1（第二版）录音MP3"
"""
import argparse
import glob
import json
import os
import sys
import zipfile

GAP_SPLIT = 2.5      # 大于该静音间隔就切一个新组（秒）
TARGET = 30.0        # 单组最长时长（秒），到顶也切

# 教材《大学通用俄语1（第二版）》已知发音内容（用于标注，循环复用）
ALPHA = ["А","Б","В","Г","Д","Е","Ё","Ж","З","И","Й","К","Л","М","Н","О",
         "П","Р","С","Т","У","Ф","Х","Ц","Ч","Ш","Щ","Ъ","Ы","Ь","Э","Ю","Я"]
L2 = ["ба","ва","га","да","жа","за","ка","ла","ма","на"]


def group(segs):
    """把细粒度语音段按 静音间隔/最大时长 归并成「练习/句」级段落。"""
    if not segs:
        return []
    g = []
    cs, ce = segs[0][0], segs[0][1]
    for s in segs[1:]:
        if (s[0] - ce) > GAP_SPLIT or (ce - cs) >= TARGET:
            g.append([round(cs, 3), round(ce, 3)])
            cs, ce = s[0], s[1]
        else:
            ce = s[1]
    g.append([round(cs, 3), round(ce, 3)])
    return g


def label_for(n, idx):
    if n == 1:
        return ALPHA[idx % len(ALPHA)]
    if n == 2:
        return L2[idx % len(L2)]
    return "урок %d · %d" % (n, idx + 1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="textbook.zip")
    ap.add_argument("--audio-dir", required=True)
    ap.add_argument("--seg-dir", default="/tmp")
    args = ap.parse_args()

    lessons = []
    for n in range(1, 12):
        segfile = os.path.join(args.seg_dir, "seg_%d.json" % n)
        if not os.path.exists(segfile):
            print("[warn] 缺少 %s，跳过 урок %d" % (segfile, n))
            continue
        d = json.load(open(segfile, encoding="utf-8"))
        grps = group(d["segments"])
        segs = [{"i": i + 1, "ru": label_for(n, i),
                 "start": g[0], "end": g[1]} for i, g in enumerate(grps)]
        lessons.append({
            "n": n,
            "title": "урок %d · 第%d课" % (n, n),
            "audio": "audio/урок %d.mp3" % n,
            "segments": segs,
        })

    manifest = {"lessons": lessons}
    out = args.out
    if os.path.exists(out):
        os.remove(out)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
        for les in lessons:
            base = os.path.basename(les["audio"])
            src = os.path.join(args.audio_dir, base)
            if os.path.exists(src):
                z.write(src, les["audio"])
            else:
                print("[warn] 找不到音频 %s" % src)
    total_seg = sum(len(l["segments"]) for l in lessons)
    print("已生成 %s：%d 课，%d 段，%.1f MB" % (
        out, len(lessons), total_seg, os.path.getsize(out) / 1e6))


if __name__ == "__main__":
    main()
