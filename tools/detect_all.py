#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""对 11 个 урок mp3 跑静音检测，输出 /tmp/seg_1.json ... /tmp/seg_11.json
以及汇总 /tmp/seg_summary.json。"""
import json
import os
import subprocess
import sys
import wave
import math
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from detect_segments import decode, energy_windows, detect  # reuse

AUDIO_DIR = "/Users/davey/Downloads/rar_extract/大学通用俄语1（第二版）录音MP3"

def main():
    summary = {}
    for n in range(1, 12):
        name = "урок %d.mp3" % n
        path = os.path.join(AUDIO_DIR, name)
        if not os.path.exists(path):
            print("missing", name); continue
        wav = decode(path)
        energies, fr = energy_windows(wav)
        segs = detect(energies, fr)
        dur = len(energies) * (512 / fr)
        os.remove(wav)
        out = "/tmp/seg_%d.json" % n
        with open(out, "w", encoding="utf-8") as f:
            json.dump({"n": n, "duration": round(dur, 3), "segments": segs}, f, ensure_ascii=False)
        summary[str(n)] = {"duration": round(dur, 1), "segments": len(segs)}
        print("урок %d: dur=%.1fs segs=%d" % (n, dur, len(segs)))
    with open("/tmp/seg_summary.json", "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    main()
