package com.example.rulearn.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 词书持久化。用户词库全部来自「导入 / 下载」，App 不内置任何词库。
 * 支持 JSON 数组与 TSV/CSV 两种 UTF-8 文本格式。
 */
object BookStore {

    private const val FILE = "vocab.json"

    fun load(ctx: Context): List<WordBook> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(f.readText())
            val out = mutableListOf<WordBook>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                val entries = entriesFrom(o.optJSONArray("entries"))
                if (entries.isEmpty()) continue
                out.add(WordBook(id, o.optString("name", id), BookKind.VOCAB, entries))
            }
            out
        }.getOrDefault(emptyList())
    }

    fun save(ctx: Context, books: List<WordBook>) {
        val arr = JSONArray()
        books.filter { it.kind == BookKind.VOCAB }.forEach { b ->
            val o = JSONObject().put("id", b.id).put("name", b.name)
            val ea = JSONArray()
            b.entries.forEach { e ->
                ea.put(
                    JSONObject()
                        .put("ru", e.ru).put("zh", e.zh)
                        .put("audio", e.audio).put("img", e.img).put("note", e.note)
                )
            }
            arr.put(o.put("entries", ea))
        }
        runCatching { File(ctx.filesDir, FILE).writeText(arr.toString()) }
    }

    /** 从本地文件导入一本新词书，返回词书名；失败返回 null。 */
    fun importBook(ctx: Context, uri: Uri, name: String? = null): String? {
        val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
        val entries = parse(text)
        if (entries.isEmpty()) return null
        return addBook(ctx, entries, name ?: fileName(uri) ?: "导入词库")
    }

    /** 从 URL 下载一本新词书，返回词书名；失败返回 null。 */
    fun downloadBook(ctx: Context, url: String, name: String? = null): String? {
        val text = fetchUrl(url) ?: return null
        val entries = parse(text)
        if (entries.isEmpty()) return null
        val fallback = url.substringAfterLast('/').substringBefore('?')
            .removeSuffix(".json").removeSuffix(".tsv").removeSuffix(".txt")
            .takeIf { it.isNotBlank() } ?: "下载词库"
        return addBook(ctx, entries, name?.takeIf { it.isNotBlank() } ?: fallback)
    }

    private fun addBook(ctx: Context, entries: List<WordEntry>, name: String): String {
        val id = "b_${System.currentTimeMillis()}"
        val books = load(ctx).toMutableList()
        books.add(WordBook(id, name, BookKind.VOCAB, entries))
        save(ctx, books)
        return name
    }

    fun delete(ctx: Context, id: String) {
        save(ctx, load(ctx).filter { it.id != id })
    }

    /** 解析 JSON 数组或 TSV/CSV 文本。 */
    fun parse(text: String): List<WordEntry> {
        val t = text.trim()
        if (t.startsWith("[")) return parseJson(t)
        return parseTable(t)
    }

    private fun parseJson(t: String): List<WordEntry> {
        val out = LinkedHashMap<String, WordEntry>()
        val arr = runCatching { JSONArray(t) }.getOrElse {
            // 兼容 {"words":[...]} / {"data":[...]} 包装
            val o = runCatching { JSONObject(t) }.getOrNull() ?: return emptyList()
            listOf("words", "data", "list", "items").firstNotNullOfOrNull { k ->
                o.optJSONArray(k)
            } ?: return emptyList()
        }
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val ru = e.optString("ru", e.optString("word", e.optString("text", ""))).trim()
            if (ru.isEmpty()) continue
            val zh = e.optString("zh", e.optString("mean", e.optString("meaning", e.optString("trans", "")))).trim()
            if (zh.isEmpty()) continue
            val key = ru.lowercase()
            if (out.containsKey(key)) continue
            out[key] = WordEntry(
                ru = ru,
                zh = zh,
                audio = e.optString("audio", ""),
                img = e.optString("img", e.optString("image", "")),
                note = e.optString("note", "")
            )
        }
        return out.values.toList()
    }

    /** 从持久化数组还原词条列表。 */
    private fun entriesFrom(arr: JSONArray?): List<WordEntry> {
        if (arr == null) return emptyList()
        val out = ArrayList<WordEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val ru = e.optString("ru").trim()
            if (ru.isEmpty()) continue
            out.add(
                WordEntry(
                    ru = ru,
                    zh = e.optString("zh"),
                    audio = e.optString("audio"),
                    img = e.optString("img"),
                    note = e.optString("note")
                )
            )
        }
        return out
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

    private fun fileName(uri: Uri): String? =
        uri.lastPathSegment?.substringAfterLast('/')?.substringBefore('?')
            ?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
}
