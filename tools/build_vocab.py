#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从已转写的《大学通用俄语》课文里提取词汇，生成【独立】的单词练习资源包。

与课本包（textbook.zip，课文+音频时间轴）分开，避免语音课里的单字母被当成单词练。

产出：JSON 数组，字段 ru（俄语）、zh（汉译）、audio（读音地址）。
读音用有道 dictvoice（免密钥），俄语必须带 le=ru，否则服务端返回 null audio。

用法：
  python3 tools/build_vocab.py              # 默认取高频 300 词
  python3 tools/build_vocab.py --top 500
  python3 tools/build_vocab.py --min-len 3
"""
import argparse
import json
import os
import re
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOLS = os.path.join(ROOT, "tools")
MANIFEST = os.path.join(TOOLS, "manifest_transcribed.json")
CACHE = os.path.join(TOOLS, "vocab_trans_cache.json")
OUT_JSON = os.path.join(TOOLS, "vocab.json")

UA = "RuLearn/1.0"
HEADERS = {"User-Agent": UA, "Referer": "https://www.youdao.com/"}
CYR = re.compile(r"[А-Яа-яЁё]+")


def audio_url(word: str) -> str:
    """有道网络发音：俄语必须 le=ru。"""
    return "https://dict.youdao.com/dictvoice?audio=%s&le=ru" % urllib.parse.quote(word)


def translate(word: str, cache: dict) -> str:
    """有道免密钥俄译中，失败返回空串。"""
    if word in cache:
        return cache[word]
    url = "https://aidemo.youdao.com/trans?q=%s&from=ru&to=zh-CHS" % urllib.parse.quote(word)
    for _ in range(3):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=12) as r:
                data = json.loads(r.read().decode("utf-8"))
            tr = data.get("translation") or []
            zh = " ".join(x for x in tr if x).strip()
            if zh and zh != word:
                cache[word] = zh
                return zh
            break
        except Exception:
            time.sleep(0.6)
    cache[word] = ""
    return ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=300, help="取高频前 N 个词")
    ap.add_argument("--min-len", type=int, default=3, help="最短词长（滤掉单字母与音节）")
    args = ap.parse_args()

    m = json.load(open(MANIFEST, encoding="utf-8"))

    # 1) 从课文中抽候选词（按出现频次）
    freq = {}
    for l in m["lessons"]:
        for seg in l["segments"]:
            ru = seg.get("ru") or ""
            for tok in CYR.findall(ru.lower()):
                if len(tok) >= args.min_len:
                    freq[tok] = freq.get(tok, 0) + 1
    cands = sorted(freq.items(), key=lambda kv: (-kv[1], kv[0]))
    print("候选词 %d 个（长度>=%d）" % (len(cands), args.min_len))

    # 2) 逐个取汉译（带缓存，避免重复请求）
    cache = {}
    if os.path.exists(CACHE):
        cache = json.load(open(CACHE, encoding="utf-8"))

    out = []
    tried = 0
    for word, cnt in cands:
        if len(out) >= args.top:
            break
        tried += 1
        zh = translate(word, cache)
        if not zh:
            continue
        out.append({"ru": word, "zh": zh, "audio": audio_url(word)})
        if len(out) % 25 == 0:
            print("  已收 %d 词（已试 %d）" % (len(out), tried), flush=True)
        # 每 20 个落盘一次，防止中断丢失
        if len(out) % 20 == 0:
            json.dump(cache, open(CACHE, "w", encoding="utf-8"), ensure_ascii=False)
    json.dump(cache, open(CACHE, "w", encoding="utf-8"), ensure_ascii=False)

    json.dump(out, open(OUT_JSON, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("\n生成 %d 条词条 -> %s" % (len(out), OUT_JSON))
    for e in out[:8]:
        print("  %-16s %-14s %s" % (e["ru"], e["zh"][:14], e["audio"][:60]))


if __name__ == "__main__":
    main()
