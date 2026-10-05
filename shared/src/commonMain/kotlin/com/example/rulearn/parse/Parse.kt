package com.example.rulearn.parse

import com.example.rulearn.BookKind
import com.example.rulearn.Lesson
import com.example.rulearn.Segment
import com.example.rulearn.WordBook
import com.example.rulearn.WordEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 跨平台 JSON 解析（kotlinx-serialization，宽松模式）。 */
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

private fun JsonElement.asObject(): JsonObject? = if (this is JsonObject) this else null
private fun JsonElement.asArray(): JsonArray? = if (this is JsonArray) this else null

private fun str(el: JsonElement?, vararg keys: String): String {
    val o = (el as? JsonObject) ?: return ""
    for (k in keys) {
        val v = o[k] ?: continue
        if (v !is kotlinx.serialization.json.JsonPrimitive) continue
        val s = v.content.trim()
        if (s.isNotEmpty()) return s
    }
    return ""
}

/**
 * 解析词库文本（JSON 数组 / 包装对象 / TSV-CSV）。返回去重后的词条。
 */
fun parseVocab(text: String): List<WordEntry> {
    val t = text.trim()
    if (t.startsWith("[")) return parseVocabJson(json.parseToJsonElement(t))
    if (t.startsWith("{")) {
        val root = json.parseToJsonElement(t)
        val arr = root.asObject()?.let { o ->
            listOf("words", "data", "list", "items").firstNotNullOfOrNull { o[it]?.asArray() }
        } ?: return emptyList()
        return parseVocabJson(arr)
    }
    return parseTable(t)
}

private fun parseVocabJson(arr: JsonArray): List<WordEntry> {
    val out = LinkedHashMap<String, WordEntry>()
    for (el in arr) {
        val o = el.asObject() ?: continue
        val ru = str(o, "ru", "word", "text").trim()
        if (ru.isEmpty()) continue
        val zh = str(o, "zh", "mean", "meaning", "trans").trim()
        if (zh.isEmpty()) continue
        val key = ru.lowercase()
        if (out.containsKey(key)) continue
        out[key] = WordEntry(
            ru = ru,
            zh = zh,
            audio = str(o, "audio"),
            img = str(o, "img", "image"),
            note = str(o, "note")
        )
    }
    return out.values.toList()
}

private fun parseTable(t: String): List<WordEntry> {
    val out = LinkedHashMap<String, WordEntry>()
    t.lines().forEachIndexed { idx, raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@forEachIndexed
        val low = line.lowercase()
        if (idx == 0 && (low == "ru" || low.startsWith("ru\t") || low.startsWith("ru,") ||
                    low.startsWith("ru;") || low.startsWith("ru "))
        ) return@forEachIndexed
        val parts = when {
            line.contains("\t") -> line.split("\t")
            line.contains(",") -> line.split(",")
            line.contains(";") -> line.split(";")
            else -> return@forEachIndexed
        }
        if (parts.size < 2) return@forEachIndexed
        val ru = parts[0].trim()
        val zh = parts[1].trim()
        if (ru.isEmpty() || zh.isEmpty()) return@forEachIndexed
        val key = ru.lowercase()
        if (out.containsKey(key)) return@forEachIndexed
        out[key] = WordEntry(
            ru = ru,
            zh = zh,
            audio = parts.getOrNull(2)?.trim().orEmpty(),
            img = parts.getOrNull(3)?.trim().orEmpty()
        )
    }
    return out.values.toList()
}

/** 把词书集合序列化为 JSON 文本（仅 VOCAB）。 */
fun serializeVocab(books: List<WordBook>): String {
    val root = json.buildJsonArray {
        books.filter { it.kind == BookKind.VOCAB }.forEach { b ->
            add(json.buildJsonObject {
                put("id", b.id); put("name", b.name)
                put("entries", json.buildJsonArray {
                    b.entries.forEach { e ->
                        add(json.buildJsonObject {
                            put("ru", e.ru); put("zh", e.zh)
                            put("audio", e.audio); put("img", e.img); put("note", e.note)
                        })
                    }
                })
            })
        }
    }
    return root.toString()
}

/** 解析课程文本（{"lessons":[...]} / 裸数组 / 清单）。 */
fun parseLessons(text: String): List<Lesson> {
    val t = text.trim()
    val arr: JsonArray = when {
        t.startsWith("[") -> json.parseToJsonElement(t).asArray() ?: return emptyList()
        t.startsWith("{") -> {
            val root = json.parseToJsonElement(t).asObject() ?: return emptyList()
            root["lessons"]?.asArray()
                ?: root["data"]?.asArray()
                ?: root["list"]?.asArray()
                ?: return emptyList()
        }
        else -> return emptyList()
    }
    val out = mutableListOf<Lesson>()
    arr.forEachIndexed { i, el ->
        val o = el.asObject() ?: return@forEachIndexed
        val n = o["n"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: (i + 1)
        val title = str(o, "title").ifBlank { "урок $n" }
        val lessonAudio = str(o, "audio")
        val segsEl = o["segments"]?.asArray() ?: return@forEachIndexed
        val segments = mutableListOf<Segment>()
        segsEl.forEachIndexed { j, s ->
            val so = s.asObject() ?: return@forEachIndexed
            val ru = str(so, "ru", "text").trim()
            if (ru.isEmpty()) return@forEachIndexed
            val start = so["start"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() } ?: 0.0
            val end = so["end"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() } ?: 0.0
            val dur = so["dur"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() } ?: 0.0
            segments.add(
                Segment(
                    i = so["i"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: (j + 1),
                    ru = ru,
                    zh = str(so, "zh", "mean"),
                    dur = if (dur > 0) dur else (end - start),
                    audio = str(so, "audio"),
                    start = start,
                    end = end
                )
            )
        }
        if (segments.isNotEmpty()) out.add(Lesson(n, title, segments, lessonAudio))
    }
    return out
}

/** 把课程集合序列化为 JSON 文本。 */
fun serializeLessons(lessons: List<Lesson>): String {
    val root = json.buildJsonArray {
        lessons.forEach { l ->
            add(json.buildJsonObject {
                put("n", l.n); put("title", l.title); put("audio", l.audio)
                put("segments", json.buildJsonArray {
                    l.segments.forEach { s ->
                        add(json.buildJsonObject {
                            put("i", s.i); put("ru", s.ru); put("zh", s.zh)
                            put("dur", s.dur); put("audio", s.audio)
                            put("start", s.start); put("end", s.end)
                        })
                    }
                })
            })
        }
    }
    return root.toString()
}
