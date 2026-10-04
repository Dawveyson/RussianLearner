package com.example.rulearn.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 教材（课文）持久化。同样不内置，由用户导入课程 JSON。
 * 支持 {"lessons":[...]} 与裸数组两种形状。
 */
object LessonStore {

    private const val FILE = "lessons.json"

    fun load(ctx: Context): List<Lesson> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return runCatching { parseText(f.readText()) }.getOrDefault(emptyList())
    }

    fun save(ctx: Context, lessons: List<Lesson>) {
        val arr = JSONArray()
        lessons.forEach { l ->
            val o = JSONObject().put("n", l.n).put("title", l.title).put("audio", l.audio)
            val segs = JSONArray()
            l.segments.forEach { s ->
                segs.put(
                    JSONObject().put("i", s.i).put("ru", s.ru).put("zh", s.zh)
                        .put("dur", s.dur).put("audio", s.audio)
                        .put("start", s.start).put("end", s.end)
                )
            }
            arr.put(o.put("segments", segs))
        }
        runCatching { File(ctx.filesDir, FILE).writeText(arr.toString()) }
    }

    /**
     * 从 zip 音频书导入。
     *
     * 包结构见 [ImportSpec.ZIP_SPEC]：根目录放一个清单 JSON，音频放在
     * audio/（或任意子目录）里，清单里写相对路径。本方法会：
     *  1. 解压到 filesDir/books/<包名>/；
     *  2. 把清单里的相对音频路径改写成解压后的本机绝对路径；
     *  3. 与已有课程按课号合并（同课号覆盖）。
     *
     * @return 导入的课数，0 表示包不合规范。
     */
    fun importZip(ctx: Context, uri: Uri): Int {
        val dest = File(ctx.filesDir, "books/${System.currentTimeMillis()}")
        val ok = runCatching { unzip(ctx, uri, dest) }.getOrDefault(false)
        if (!ok) return 0

        val manifest = dest.walkTopDown().firstOrNull { f ->
            f.isFile && f.name.lowercase() in MANIFEST_NAMES
        } ?: return 0

        val parsed = parseText(manifest.readText())
        if (parsed.isEmpty()) return 0

        // 把清单里的相对路径变成解压目录里的真实文件
        val resolved = resolveAssets(parsed, dest)
        val merged = merge(load(ctx), resolved)
        save(ctx, merged)
        return resolved.size
    }

    /** zip 里可能出现的清单文件名（小写匹配）。 */
    private val MANIFEST_NAMES = setOf("book.json", "lessons.json", "manifest.json", "index.json")

    /**
     * 解压 zip 到 [dest]。
     * 做了 zip slip 防护：任何成员的目标路径都必须落在 dest 目录内。
     */
    private fun unzip(ctx: Context, uri: Uri, dest: File): Boolean {
        val input = ctx.contentResolver.openInputStream(uri) ?: return false
        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zis ->
                dest.mkdirs()
                val destRoot = dest.canonicalFile
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = File(dest, entry.name)
                    // zip slip：../../ 之类的成员名会把文件写到 dest 之外，必须拒绝
                    val inBounds = target.canonicalFile.path.startsWith(destRoot.path + File.separator) ||
                        target.canonicalFile == destRoot
                    if (inBounds) {
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zis.copyTo(it) }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return true
    }

    /** 清单里的音频既可以是 http URL，也可以是包内的相对路径。 */
    private fun resolveAssets(lessons: List<Lesson>, base: File): List<Lesson> =
        lessons.map { l ->
            l.copy(
                audio = resolveOne(base, l.audio),
                segments = l.segments.map { s -> s.copy(audio = resolveOne(base, s.audio)) }
            )
        }

    private fun resolveOne(base: File, path: String): String {
        if (path.isBlank()) return ""
        // 网络地址、本机绝对路径原样保留
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("/") || path.startsWith("file://")
        ) return path
        val f = File(base, path)
        // 找不到文件就保留原样，让播放器回落到 assets / 网络发音
        return if (f.exists()) f.absolutePath else path
    }

    /** 从本地文件导入课程，同课号覆盖、其余追加。返回课数。 */
    fun importFrom(ctx: Context, uri: Uri): Int {
        val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return 0
        val parsed = parseText(text)
        if (parsed.isEmpty()) return 0
        val merged = merge(load(ctx), parsed)
        save(ctx, merged)
        return parsed.size
    }

    fun delete(ctx: Context, n: Int) {
        save(ctx, load(ctx).filter { it.n != n })
    }

    fun merge(base: List<Lesson>, incoming: List<Lesson>): List<Lesson> {
        val byN = base.associateBy { it.n }.toMutableMap()
        incoming.forEach { byN[it.n] = it }
        return byN.values.sortedBy { it.n }
    }

    private fun parseText(text: String): List<Lesson> {
        val t = text.trim()
        val arr = when {
            t.startsWith("[") -> JSONArray(t)
            t.startsWith("{") -> {
                val root = JSONObject(t)
                listOf("lessons", "data", "list").firstNotNullOfOrNull { root.optJSONArray(it) }
                    ?: JSONArray()
            }
            else -> JSONArray()
        }
        val out = mutableListOf<Lesson>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val n = o.optInt("n", i + 1)
            val title = o.optString("title", "урок $n")
            val lessonAudio = o.optString("audio", "")
            val segs = o.optJSONArray("segments") ?: JSONArray()
            val segments = mutableListOf<Segment>()
            for (j in 0 until segs.length()) {
                val s = segs.optJSONObject(j) ?: continue
                val ru = s.optString("ru", s.optString("text", "")).trim()
                if (ru.isEmpty()) continue
                val start = s.optDouble("start", 0.0)
                val end = s.optDouble("end", 0.0)
                val dur = s.optDouble("dur", 0.0)
                segments.add(
                    Segment(
                        i = s.optInt("i", segments.size + 1),
                        ru = ru,
                        zh = s.optString("zh", s.optString("mean", "")),
                        dur = if (dur > 0) dur else (end - start),
                        audio = s.optString("audio", ""),
                        start = start,
                        end = end
                    )
                )
            }
            if (segments.isNotEmpty()) out.add(Lesson(n, title, segments, lessonAudio))
        }
        return out
    }
}
