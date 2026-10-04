#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把《大学通用俄语1（第二版）》录音 MP3 处理成 App 可用的「教材 ZIP」（音频 + manifest）。

产物 textbook.zip 内部结构：
    textbook.zip
    ├── manifest.json          ← App 的 ZIP 导入会自动找到它
    └── audio/
        ├── урок 1.mp3
        ├── ...
        └── урок 11.mp3

manifest 每段带 start/end 时间戳，App 的「随身听」据此做歌词时间轴 + 点击跳转。

用法
----
1) 准备文本稿（每个 урок 被朗读的字母/音节，顺序与录音一致）。两种写法：

   写法 A（只给文本，时间由脚本用 ffmpeg 静音检测或均分生成）：
   {
     "урок 1": ["А","Б","В","Г", ...],
     "урок 2": ["ба","ва","га", ...]
   }

   写法 B（连时间一起给，最准）：
   {
     "урок 1": [
        {"ru":"А","start":0.0,"end":1.3},
        {"ru":"Б","start":1.3,"end":2.6}
     ]
   }

   也可混用：某一课用 A，另一课用 B。

2) 运行：
   python3 build_textbook_zip.py \
       --audio-dir "/Users/davey/Downloads/rar_extract/大学通用俄语1（第二版）录音MP3" \
       --transcript transcript.json \
       --out textbook.zip

   --ffmpeg 可指定 ffmpeg 路径（默认从 PATH 取）。没有 ffmpeg 时会退化为「按行数均分时长」，
   也能用，只是同步是近似的。

文本稿里没出现的 урок 不会进包；音频目录里没对应 mp3 的课会被跳过并告警。
"""

import argparse
import json
import os
import re
import subprocess
import sys
import zipfile


def find_audio(audio_dir, n):
    """按课号 n 在 audio_dir 里找 'урок N.mp3'（容忍空格/无空格、大小写）。"""
    candidates = []
    for name in os.listdir(audio_dir):
        m = re.match(r"урок[ _]?0*%d\.mp3$" % n, name, re.IGNORECASE)
        if m:
            candidates.append(name)
    if not candidates:
        return None
    # 优先精确 "урок N.mp3"
    for c in candidates:
        if c.lower() == "урок %d.mp3" % n:
            return os.path.join(audio_dir, c)
    return os.path.join(audio_dir, candidates[0])


def get_duration_sec(ffmpeg, path):
    """用 ffprobe 取时长（秒）。没有 ffmpeg 或失败返回 None。"""
    if not ffmpeg:
        return None
    try:
        ffprobe = ffmpeg.replace("ffmpeg", "ffprobe")
        out = subprocess.run(
            [ffprobe, "-v", "error", "-show_entries", "format=duration",
             "-of", "default=noprint_wrappers=1:nokey=1", path],
            capture_output=True, text=True, check=True,
        )
        return float(out.stdout.strip())
    except Exception:
        return None


def detect_silence(ffmpeg, path):
    """用 ffmpeg silencedetect 返回「静音区间」列表 [(start,end), ...]。"""
    if not ffmpeg:
        return []
    try:
        out = subprocess.run(
            [ffmpeg, "-i", path, "-af",
             "silencedetect=noise=-30dB:d=0.25", "-f", "null", "-"],
            capture_output=True, text=True,
        )
        text = out.stderr
        starts, ends = [], []
        for line in text.splitlines():
            m = re.search(r"silence_start: ([\d.]+)", line)
            if m:
                starts.append(float(m.group(1)))
            m = re.search(r"silence_end: ([\d.]+)", line)
            if m:
                ends.append(float(m.group(1)))
        # 拼出静音区间
        silences = []
        for i in range(min(len(starts), len(ends))):
            silences.append((starts[i], ends[i]))
        return silences
    except Exception:
        return []


def build_segments(lines, dur, ffmpeg, path):
    """根据文本行 + 时长生成带时间戳的 segments。

    - 若某行自带 start/end，原样采用；
    - 否则优先用静音检测把音轨切成「语音块」，块数与行数一致时直接对应；
    - 否则按行数在总时长内均分。
    """
    explicit = []
    for ln in lines:
        if isinstance(ln, dict) and "start" in ln and "end" in ln:
            explicit.append((ln["ru"], float(ln["start"]), float(ln["end"])))
    if len(explicit) == len(lines) and explicit:
        return [{"i": i + 1, "ru": r, "start": s, "end": e}
                for i, (r, s, e) in enumerate(explicit)]

    n = len(lines)
    if dur is None or dur <= 0:
        # 没有时长也无法静音检测：给一个占位（每段 2.5s 递增），App 仍可播放整轨。
        return [{"i": i + 1, "ru": _text_of(ln), "start": round(i * 2.5, 2),
                 "end": round((i + 1) * 2.5, 2)} for i, ln in enumerate(lines)]

    silences = detect_silence(ffmpeg, path)
    # 由静音区间推导语音块边界
    bounds = [0.0]
    for (s, e) in silences:
        bounds.append(s)
        bounds.append(e)
    bounds.append(dur)
    # 相邻去重并排序
    bounds = sorted(set(round(b, 3) for b in bounds))
    chunks = [(bounds[k], bounds[k + 1]) for k in range(0, len(bounds) - 1, 2)]
    chunks = [c for c in chunks if c[1] - c[0] > 0.05]

    segs = []
    if len(chunks) == n:
        for i, ln in enumerate(lines):
            s, e = chunks[i]
            segs.append({"i": i + 1, "ru": _text_of(ln), "start": round(s, 2),
                         "end": round(e, 2)})
    else:
        # 块数与行数对不上 → 均分
        for i, ln in enumerate(lines):
            s = dur * i / n
            e = dur * (i + 1) / n
            segs.append({"i": i + 1, "ru": _text_of(ln), "start": round(s, 2),
                         "end": round(e, 2)})
    return segs


def _text_of(ln):
    if isinstance(ln, dict):
        return ln.get("ru", ln.get("text", "")).strip()
    return str(ln).strip()


def main():
    ap = argparse.ArgumentParser(description="把俄语教材录音打包成 App 教材 ZIP")
    ap.add_argument("--audio-dir", required=True, help="解压后的 MP3 目录")
    ap.add_argument("--transcript", required=True, help="文本稿 JSON 路径")
    ap.add_argument("--out", default="textbook.zip", help="输出 zip 路径")
    ap.add_argument("--ffmpeg", default="ffmpeg", help="ffmpeg 可执行路径（默认取 PATH）")
    args = ap.parse_args()

    # 校验 ffmpeg 是否真的可用
    ffmpeg = None
    if args.ffmpeg and os.path.exists(args.ffmpeg):
        ffmpeg = args.ffmpeg
    else:
        try:
            subprocess.run([args.ffmpeg, "-version"], capture_output=True, check=True)
            ffmpeg = args.ffmpeg
        except Exception:
            print("[warn] 未找到 ffmpeg，时间戳将退化为「按行数均分」（近似同步）。", file=sys.stderr)

    with open(args.transcript, "r", encoding="utf-8") as f:
        transcript = json.load(f)

    lessons = []
    for raw_key, lines in transcript.items():
        m = re.search(r"(\d+)", raw_key)
        if not m:
            print("[skip] 无法从键 %r 解析课号" % raw_key, file=sys.stderr)
            continue
        n = int(m.group(1))
        if not lines:
            continue
        audio = find_audio(args.audio_dir, n)
        if not audio:
            print("[warn] урок %d 找不到对应 mp3，跳过" % n, file=sys.stderr)
            continue
        dur = get_duration_sec(ffmpeg, audio)
        segs = build_segments(lines, dur, ffmpeg, audio)
        rel = "audio/" + os.path.basename(audio)
        lessons.append({
            "n": n,
            "title": "урок %d · 第%d课" % (n, n),
            "audio": rel,
            "segments": segs,
        })

    lessons.sort(key=lambda x: x["n"])
    manifest = {"lessons": lessons}

    # 写 zip
    out = args.out
    if os.path.exists(out):
        os.remove(out)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
        for les in lessons:
            apath = os.path.join(args.audio_dir, os.path.basename(les["audio"]))
            if os.path.exists(apath):
                z.write(apath, les["audio"])
    print("已生成 %s，共 %d 课，%d 段。" % (
        out, len(lessons), sum(len(l["segments"]) for l in lessons)))


if __name__ == "__main__":
    main()
