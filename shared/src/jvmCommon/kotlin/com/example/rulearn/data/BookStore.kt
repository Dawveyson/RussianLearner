package com.example.rulearn.data

import com.example.rulearn.BookKind
import com.example.rulearn.WordBook
import com.example.rulearn.WordEntry
import com.example.rulearn.parse.parseVocab
import com.example.rulearn.parse.serializeVocab
import com.example.rulearn.platform.readText
import com.example.rulearn.platform.writeText

/**
 * 词库持久化。用户词库全部来自「导入 / 下载」，App 不内置任何词库。
 * 支持 JSON 数组与 TSV/CSV 两种 UTF-8 文本格式。
 */
object BookStore {

    private const val FILE = "vocab.json"

    fun loadBooks(dir: String): List<WordBook> {
        val text = readText("$dir/$FILE") ?: return emptyList()
        return runCatching {
            val arr = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonArray
            val out = mutableListOf<WordBook>()
            for (el in arr) {
                val o = el.jsonObject
                val id = o["id"]?.toString()?.trim('"') ?: continue
                if (id.isBlank()) continue
                val entries = entriesFrom(o["entries"])
                if (entries.isEmpty()) continue
                out.add(WordBook(id, o["name"]?.toString()?.trim('"') ?: id, BookKind.VOCAB, entries))
            }
            out
        }.getOrDefault(emptyList())
    }

    fun saveBooks(dir: String, books: List<WordBook>) {
        writeText("$dir/$FILE", serializeVocab(books))
    }

    /** 解析文本并新增一本词书，返回词书名；失败返回 null。 */
    fun importBookText(dir: String, text: String, name: String? = null): String? {
        val entries = parseVocab(text)
        if (entries.isEmpty()) return null
        return addBook(dir, entries, name ?: "导入词库")
    }

    fun downloadBook(dir: String, url: String, name: String? = null): String? {
        val text = OnlineDict.fetchUrl(url) ?: return null
        val entries = parseVocab(text)
        if (entries.isEmpty()) return null
        val fallback = url.substringAfterLast('/').substringBefore('?')
            .removeSuffix(".json").removeSuffix(".tsv").removeSuffix(".txt")
            .takeIf { it.isNotBlank() } ?: "下载词库"
        return addBook(dir, entries, name?.takeIf { it.isNotBlank() } ?: fallback)
    }

    fun deleteBook(dir: String, id: String) {
        saveBooks(dir, loadBooks(dir).filter { it.id != id })
    }

    private fun entriesFrom(el: kotlinx.serialization.json.JsonElement?): List<WordEntry> {
        val arr = (el as? kotlinx.serialization.json.JsonArray) ?: return emptyList()
        val out = ArrayList<WordEntry>(arr.size)
        for (e in arr) {
            val o = e.jsonObject
            val ru = (o["ru"]?.toString() ?: "").trim('"').trim()
            if (ru.isEmpty()) continue
            out.add(
                WordEntry(
                    ru = ru,
                    zh = (o["zh"]?.toString() ?: "").trim('"'),
                    audio = (o["audio"]?.toString() ?: "").trim('"'),
                    img = (o["img"]?.toString() ?: "").trim('"'),
                    note = (o["note"]?.toString() ?: "").trim('"')
                )
            )
        }
        return out
    }

    private fun addBook(dir: String, entries: List<WordEntry>, name: String): String {
        val id = "b_${System.currentTimeMillis()}"
        val books = loadBooks(dir).toMutableList()
        books.add(WordBook(id, name, BookKind.VOCAB, entries))
        saveBooks(dir, books)
        return name
    }
}
