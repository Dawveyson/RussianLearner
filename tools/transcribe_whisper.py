#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 Whisper 转写教材音频，并把识别出的真实俄文按「已有的精确时间戳」对齐到各段，
然后重新生成资源包 textbook.zip。

设计要点：
  1. 时间戳沿用 tools/manifest.json 里基于静音检测切出的精确边界（不重新切分），
     只把每段窗口内的 ASR 文本按时间重叠填进去，保证声音与文本严格对应。
  2. ASR 结果缓存到 tools/asr_cache.json，可用 --realign 只重对齐、不再跑模型。

用法：
  python3 tools/transcribe_whisper.py                  # 默认 small/int8 全量
  python3 tools/transcribe_whisper.py --model medium    # 更高精度（更慢）
  python3 tools/transcribe_whisper.py --lessons 1,2     # 只跑指定课
  python3 tools/transcribe_whisper.py --realign         # 用缓存结果重新对齐打包
"""
import argparse
import json
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOLS = os.path.join(ROOT, "tools")
AUDIO_SRC = os.path.join(TOOLS, "audio_src")
MANIFEST_IN = os.path.join(TOOLS, "manifest.json")
ASR_CACHE = os.path.join(TOOLS, "asr_cache.json")
MANIFEST_OUT = os.path.join(TOOLS, "manifest_transcribed.json")
OUT_ZIP = os.path.join(TOOLS, "textbook.zip")


def load_manifest():
    with open(MANIFEST_IN, encoding="utf-8") as f:
        return json.load(f)


def save_manifest(m):
    with open(MANIFEST_OUT, "w", encoding="utf-8") as f:
        json.dump(m, f, ensure_ascii=False, indent=2)


def run_asr(model_size, compute_type, only):
    """用 faster-whisper 逐课转写，结果写回缓存。"""
    from faster_whisper import WhisperModel

    m = load_manifest()
    cache = {}
    if os.path.exists(ASR_CACHE):
        with open(ASR_CACHE, encoding="utf-8") as f:
            cache = json.load(f)

    print("加载模型 %s (%s)…" % (model_size, compute_type), flush=True)
    model = WhisperModel(model_size, device="cpu", compute_type=compute_type)

    for l in m["lessons"]:
        n = l["n"]
        if only and n not in only:
            continue
        audio = os.path.join(AUDIO_SRC, "lesson%d.mp3" % n)
        if not os.path.exists(audio):
            print("!! 缺少音频，跳过：%s" % audio)
            continue
        print("== 转写 урок %d ==" % n, flush=True)
        segments, info = model.transcribe(
            audio,
            language="ru",       # 教材是俄语，锁定语种避免误判
            vad_filter=True,     # 跳过静音，减少无意义输出
            beam_size=5,
        )
        out = []
        for s in segments:
            t = (s.text or "").strip()
            if t:
                out.append([round(float(s.start), 3), round(float(s.end), 3), t])
        cache[str(n)] = out
        print("   урок %d：识别 %d 条，音频时长 %.1fs" % (n, len(out), info.duration), flush=True)
        # 每课都落盘，避免中途失败全部丢失
        with open(ASR_CACHE, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False)
    return cache


def align(m, cache):
    """把 ASR 文本按时间重叠填入现有精确段（每个 ASR 片段归给中点所在的段）。"""
    total = 0
    filled = 0
    for l in m["lessons"]:
        n = str(l["n"])
        asr = cache.get(n) or []
        buckets = [[] for _ in l["segments"]]
        for (s, e, t) in asr:
            center = (s + e) / 2.0
            hit = -1
            for i, seg in enumerate(l["segments"]):
                a, b = float(seg.get("start", 0)), float(seg.get("end", 0))
                if a <= center <= b:
                    hit = i
                    break
            if hit < 0:
                # 落在段间空隙：归给紧邻的前一段
                for i, seg in enumerate(l["segments"]):
                    if float(seg.get("end", 0)) <= center:
                        hit = i
            if hit >= 0:
                buckets[hit].append(t)
        for i, seg in enumerate(l["segments"]):
            txt = " ".join(buckets[i]).replace("  ", " ").strip()
            seg["ru"] = txt
            total += 1
            if txt:
                filled += 1
    return total, filled


def repack(m):
    if os.path.exists(OUT_ZIP):
        os.remove(OUT_ZIP)
    with zipfile.ZipFile(OUT_ZIP, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("manifest.json", json.dumps(m, ensure_ascii=False, indent=2))
        for n in range(1, 12):
            p = os.path.join(AUDIO_SRC, "lesson%d.mp3" % n)
            if os.path.exists(p):
                z.write(p, "audio/lesson%d.mp3" % n)
    return OUT_ZIP


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="small", help="tiny/base/small/medium/large-v3")
    ap.add_argument("--compute", default="int8", help="int8 / float16 / float32")
    ap.add_argument("--lessons", default="", help="只跑指定课，如 1,2")
    ap.add_argument("--realign", action="store_true", help="不跑模型，用缓存重新对齐打包")
    args = ap.parse_args()

    only = set(int(x) for x in args.lessons.split(",") if x.strip())

    if args.realign:
        if not os.path.exists(ASR_CACHE):
            print("没有 ASR 缓存，无法 --realign")
            return 1
        with open(ASR_CACHE, encoding="utf-8") as f:
            cache = json.load(f)
    else:
        cache = run_asr(args.model, args.compute, only)

    m = load_manifest()
    total, filled = align(m, cache)
    save_manifest(m)
    z = repack(m)

    print("\n对齐结果：%d 段中有 %d 段拿到真实俄文（空段会被 app 自动跳过）" % (total, filled))
    print("清单：", MANIFEST_OUT)
    print("资源包：", z, "%.1f MB" % (os.path.getsize(z) / 1e6))
    # 抽样预览
    for l in m["lessons"][:1]:
        for seg in l["segments"][:5]:
            print("   [%.2f-%.2f] %s" % (seg["start"], seg["end"], seg["ru"][:60]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
