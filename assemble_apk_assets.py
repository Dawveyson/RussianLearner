import json, os, shutil, glob

BASE = "/Users/davey/WorkBuddy/2026-09-14-13-30-04/ru_urok1"
PROJ = os.path.join(BASE, "RuLearnApp")
AUDIO = os.path.join(PROJ, "app", "src", "main", "assets", "audio")

def kot(s: str) -> str:
    # produce a Kotlin-compatible string literal
    return json.dumps(s).replace("$", "${'$'}")

lessons = []
total = 0
for n in range(1, 12):
    folder = BASE if n == 1 else os.path.join(BASE, f"ru_urok{n}")
    tsv = os.path.join(folder, "segments.tsv")
    if not os.path.exists(tsv):
        print("skip урок", n); continue
    rows = open(tsv, encoding="utf-8").read().splitlines()[1:]
    segs = []
    dst_dir = os.path.join(AUDIO, f"u{n}")
    os.makedirs(dst_dir, exist_ok=True)
    for line in rows:
        p = line.split("\t")
        if len(p) < 5:
            continue
        idx, start, end, ru, zh = int(p[0]), float(p[1]), float(p[2]), p[3], p[4]
        src_clip = os.path.join(folder, "clips", f"{idx:03d}_{start:06.1f}.mp3")
        if not os.path.exists(src_clip):
            print("  WARN clip missing:", src_clip); continue
        dst = os.path.join(dst_dir, f"{idx:03d}.mp3")
        shutil.copy(src_clip, dst)
        segs.append((idx, ru, zh, round(end - start, 1), f"audio/u{n}/{idx:03d}.mp3"))
        total += 1
    lessons.append((n, segs))
    print(f"урок {n}: {len(segs)} segments, copied to audio/u{n}/")

# build Subtitles.kt
lines = []
lines.append("package com.example.rulearn.data\n")
lines.append("data class Segment(")
lines.append("    val i: Int,")
lines.append("    val ru: String,")
lines.append("    val zh: String,")
lines.append("    val dur: Double,")
lines.append("    val audio: String")
lines.append(")\n")
lines.append("data class Lesson(")
lines.append("    val n: Int,")
lines.append("    val title: String,")
lines.append("    val segments: List<Segment>")
lines.append(")\n")
lines.append("val LESSONS: List<Lesson> = listOf(")
for n, segs in lessons:
    lines.append(f'    Lesson({n}, "урок {n} · 第{n}课", listOf(')
    for (idx, ru, zh, dur, audio) in segs:
        lines.append(
            f'        Segment({idx}, {kot(ru)}, {kot(zh)}, {dur}, {kot(audio)}),'
        )
    lines.append("    )),")
lines.append(")")
open(os.path.join(PROJ, "app", "src", "main", "kotlin", "com", "example", "rulearn", "data", "Subtitles.kt"),
    "w", encoding="utf-8").write("\n".join(lines))
print("Subtitles.kt written. total segments:", total)
