#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
下载《俄语字母表》33 个字母 + 例词的发音，内嵌进 App（assets/audio/letters）。
复用 App 已有的有道发音源（le=ru），离线也能播放；不再依赖联网 TTS。

策略：
  - 字母优先用「单字符」，有道对个别孤立辅音会 500，则回退到「字母名称」（же/эм/тэ…）。
  - 多次退避重试 + 请求间隔，规避有道限流。
  - 任一文件最终失败也不致命：App 端对缺失 asset 会回退到网络发音。

产出：
    app/src/main/assets/audio/letters/letter_00.mp3 ... letter_32.mp3
    app/src/main/assets/audio/letters/word_00.mp3   ... word_32.mp3
文件名用数字序号（ASCII），避免西里尔文件名在 Android 上的编码坑。
"""
import os
import sys
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "audio", "letters")
os.makedirs(ASSETS, exist_ok=True)

UA = "RuLearn/1.0"
HEADERS = {"User-Agent": UA, "Referer": "https://www.youdao.com/"}

# (大写字母, 例词) —— 顺序与 StaticData.CYRILLIC_ALPHABET 完全一致
LETTERS = [
    ("А", "арбуз"), ("Б", "банан"), ("В", "вода"), ("Г", "город"), ("Д", "дом"),
    ("Е", "еда"), ("Ё", "ёлка"), ("Ж", "журнал"), ("З", "зима"), ("И", "игра"),
    ("Й", "йогурт"), ("К", "книга"), ("Л", "лампа"), ("М", "мама"), ("Н", "нос"),
    ("О", "окно"), ("П", "папа"), ("Р", "рука"), ("С", "солнце"), ("Т", "там"),
    ("У", "утро"), ("Ф", "фото"), ("Х", "хлеб"), ("Ц", "цветок"), ("Ч", "чай"),
    ("Ш", "школа"), ("Щ", "щи"), ("Ъ", "съезд"), ("Ы", "сын"), ("Ь", "соль"),
    ("Э", "этаж"), ("Ю", "юг"), ("Я", "яблоко"),
]

# 单字符会 500 的字母 → 字母名称 / 含该音的常用词回退
NAME_FALLBACK = {
    "Ж": "же", "К": "кот", "М": "эм", "Т": "тэ", "Ъ": "твёрдый знак",
}

MP3_MAGIC = (b"\xff\xfb", b"\xff\xf3", b"\xff\xf2", b"\xff\xf1")


def is_mp3(data: bytes) -> bool:
    return data[:2] in MP3_MAGIC or data[:3] == b"ID3" or data[:4] == b"fLaC"


def download_one(text: str, out: str, attempts: int = 8) -> bool:
    if os.path.exists(out) and os.path.getsize(out) > 400 and is_mp3(open(out, "rb").read(4)):
        return True
    url = "https://dict.youdao.com/dictvoice?audio=%s&le=ru" % urllib.parse.quote(text)
    for i in range(attempts):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            data = urllib.request.urlopen(req, timeout=25).read()
            if len(data) > 400 and is_mp3(data):
                with open(out, "wb") as f:
                    f.write(data)
                return True
        except Exception:
            pass
        time.sleep(1.0 + i * 0.6)  # 退避，规避限流
    return False


def download_letter(up: str, out: str) -> bool:
    candidates = [up]
    if up in NAME_FALLBACK:
        candidates.append(NAME_FALLBACK[up])
    for c in candidates:
        if download_one(c, out):
            return True
    return False


def main():
    ok_l = ok_w = 0
    for idx, (up, w) in enumerate(LETTERS):
        i2 = "%02d" % idx
        a = download_letter(up, os.path.join(ASSETS, "letter_%s.mp3" % i2))
        b = download_one(w, os.path.join(ASSETS, "word_%s.mp3" % i2))
        ok_l += a
        ok_w += b
        print("%s  %s letter=%s  word=%s" % (i2, up, "OK" if a else "FAIL", "OK" if b else "FAIL"),
              flush=True)
        time.sleep(1.2)
    print("\n字母 %d/%d，例词 %d/%d" % (ok_l, len(LETTERS), ok_w, len(LETTERS)))
    if ok_l < len(LETTERS) or ok_w < len(LETTERS):
        print("仍有失败（App 会回退网络发音，不影响使用）。")
        sys.exit(1)


if __name__ == "__main__":
    main()
