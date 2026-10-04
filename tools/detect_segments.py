#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 macOS 自带的 afconvert 把 MP3 解码成 WAV（无需 ffmpeg / 联网），
再用纯 Python 计算短时能量做静音检测，得到「真实、精确」的语音段边界
(start/end 秒)。这些边界就是歌词时间轴的精确时间戳。

用法:
    python3 detect_segments.py <in.mp3> [out.json]

输出 JSON: { "duration": 秒, "segments": [ [start,end], ... ] }
"""
import json
import math
import os
import subprocess
import sys
import tempfile
import wave

WIN = 512            # 约 11.6ms @44.1k，短时能量窗口
SPEECH_THR = 0.012  # 进入语音的相对能量阈值（占窗内 16bit RMS 最大值比例）
SIL_THR = 0.006     # 退出语音（迟滞，低于此才认为静音）
MIN_SPEECH = 0.18   # 最短语音段(秒)，过滤咳嗽/噪声
MIN_GAP = 0.12      # 短于该值的静音间隙并入语音（不切太碎）


def decode(mp3):
    fd, wav = tempfile.mkstemp(suffix=".wav")
    os.close(fd)
    subprocess.run(["afconvert", mp3, wav, "-f", "WAVE", "-d", "LEI16"],
                   check=True, capture_output=True)
    return wav


def energy_windows(wav):
    w = wave.open(wav, "rb")
    ch = w.getnchannels()
    fr = w.getframerate()
    n = w.getnframes()
    win = WIN
    # 读全量(文件可能很大，但 22min≈237MB，可接受)；分块累加
    energies = []
    read = 0
    step = win * ch
    buf = w.readframes(win)
    while len(buf) >= step:
        vals = [0] * win
        # 取左声道(int16)
        for i in range(win):
            o = i * ch * 2
            vals[i] = int.from_bytes(buf[o:o + 2], "little", signed=True)
        s = 0
        for v in vals:
            s += v * v
        rms = math.sqrt(s / win) / 32768.0
        energies.append(rms)
        buf = w.readframes(win)
    w.close()
    return energies, fr


def detect(energies, fr):
    win_dur = WIN / fr
    segs = []
    in_speech = False
    start = 0
    for i, e in enumerate(energies):
        if not in_speech and e > SPEECH_THR:
            in_speech = True
            start = i * win_dur
        elif in_speech and e < SIL_THR:
            in_speech = False
            end = i * win_dur
            if end - start >= MIN_SPEECH:
                segs.append([round(start, 3), round(end, 3)])
    if in_speech:
        end = len(energies) * win_dur
        if end - start >= MIN_SPEECH:
            segs.append([round(start, 3), round(end, 3)])
    # 合并过短间隙
    merged = []
    for s in segs:
        if merged and s[0] - merged[-1][1] < MIN_GAP:
            merged[-1][1] = s[1]
        else:
            merged.append(list(s))
    return merged


def main():
    mp3 = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else None
    wav = decode(mp3)
    energies, fr = energy_windows(wav)
    segs = detect(energies, fr)
    dur = len(energies) * (WIN / fr)
    os.remove(wav)
    res = {"duration": round(dur, 3), "framerate": fr,
           "window_ms": round(WIN / fr * 1000, 1), "segments": segs}
    if out:
        with open(out, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2)
        print("segments=%d duration=%.1fs -> %s" % (len(segs), dur, out))
    else:
        print("segments=%d duration=%.1fs" % (len(segs), dur))
        for i, s in enumerate(segs[:60]):
            print("  %2d  %7.2f - %7.2f  (%.2fs)" % (i + 1, s[0], s[1], s[1] - s[0]))


if __name__ == "__main__":
    main()
