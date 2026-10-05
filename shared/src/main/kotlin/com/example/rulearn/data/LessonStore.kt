package com.example.rulearn.data

import com.example.rulearn.Lesson
import com.example.rulearn.Segment
import com.example.rulearn.parse.parseLessons
import com.example.rulearn.parse.serializeLessons
import com.example.rulearn.platform.exists
import com.example.rulearn.platform.readText
import com.example.rulearn.platform.unzipTo
import com.example.rulearn.platform.writeText
import java.io.File

/**
 * 教材（课文）持久化。同样不内置，由用户导入课程 JSON 或 ZIP 音频书。
 */
object LessonStore {

    private const val FILE = "lessons.json"
    private val MANIFEST_NAMES = setOf("book.json", "lessons.json", "manifest.json", "index.json")

    fun loadLessons(dir: String): List<Lesson> {
        val text = readText("$dir/$FILE") ?: return emptyList()
        return runCatching { parseLessons(text) }.getOrDefault(emptyList())
    }

    fun saveLessons(dir: String, lessons: List<Lesson>) {
        writeText("$dir/$FILE", serializeLessons(lessons))
    }

    /** 从本地 zip 音频书导入，返回导入课数，0 表示包不合规范。 */
    fun importZip(dir: String, zipPath: String): Int {
        val dest = "$dir/books/${System.currentTimeMillis()}"
        if (!unzipTo(zipPath, dest)) return 0
        val manifest = File(dest).walkTopDown().firstOrNull { f ->
            f.isFile && f.name.lowercase() in MANIFEST_NAMES
        } ?: return 0
        val parsed = parseLessons(manifest.readText(Charsets.UTF_8))
        if (parsed.isEmpty()) return 0
        val resolved = resolveAssets(parsed, dest)
        val merged = merge(loadLessons(dir), resolved)
        saveLessons(dir, merged)
        return resolved.size
    }

    /** 清单里的音频既可以是 http URL，也可以是包内的相对路径（解析为绝对路径）。 */
    private fun resolveAssets(lessons: List<Lesson>, base: String): List<Lesson> =
        lessons.map { l ->
            l.copy(
                audio = resolveOne(base, l.audio),
                segments = l.segments.map { s -> s.copy(audio = resolveOne(base, s.audio)) }
            )
        }

    private fun resolveOne(base: String, path: String): String {
        if (path.isBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("/") || path.startsWith("file://")
        ) return path
        val f = File(base, path)
        return if (f.exists()) f.absolutePath else path
    }

    /** 从本地课程文本导入，同课号覆盖、其余追加。返回课数。 */
    fun importLessonsText(dir: String, text: String): Int {
        val parsed = parseLessons(text)
        if (parsed.isEmpty()) return 0
        saveLessons(dir, merge(loadLessons(dir), parsed))
        return parsed.size
    }

    fun deleteLesson(dir: String, n: Int) {
        saveLessons(dir, loadLessons(dir).filter { it.n != n })
    }

    fun merge(base: List<Lesson>, incoming: List<Lesson>): List<Lesson> {
        val byN = base.associateBy { it.n }.toMutableMap()
        incoming.forEach { byN[it.n] = it }
        return byN.values.sortedBy { it.n }
    }
}
