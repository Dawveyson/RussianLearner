package com.example.rulearn.data

import com.example.rulearn.BookKind
import com.example.rulearn.Lesson
import com.example.rulearn.Mastery
import com.example.rulearn.RUSSIAN_QUOTES
import com.example.rulearn.StudyStats
import com.example.rulearn.WordBook
import com.example.rulearn.WordEntry
import com.example.rulearn.parse.parseVocab
import com.example.rulearn.platform.AppPrefs
import com.example.rulearn.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全 App 单一数据源（Android 与桌面共用）。所有页面只读这里的状态流，
 * 任何写操作都通过这里落地并回流，导入 / 删除 / 练习后 UI 立即刷新。
 */
object AppRepository {

    private lateinit var dir: String
    lateinit var prefs: AppPrefs

    private val _vocabBooks = MutableStateFlow<List<WordBook>>(emptyList())
    val vocabBooks: StateFlow<List<WordBook>> = _vocabBooks.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, Mastery>>(emptyMap())
    val progress: StateFlow<Map<String, Mastery>> = _progress.asStateFlow()

    private val _stats = MutableStateFlow(StudyStats())
    val stats: StateFlow<StudyStats> = _stats.asStateFlow()

    val ready: Boolean get() = ::dir.isInitialized

    /** 数据变更钩子（Android 端接成刷新桌面小组件；桌面端忽略）。 */
    var onDataChanged: (() -> Unit)? = null

    private fun emitChange() {
        if (ready) runCatching { onDataChanged?.invoke() }
    }

    fun init(dataDir: String, store: KeyValueStore) {
        dir = dataDir
        prefs = AppPrefs(store)
        reload()
    }

    fun reload() {
        _vocabBooks.value = BookStore.loadBooks(dir)
        _lessons.value = LessonStore.loadLessons(dir)
        _progress.value = ProgressStore.load(dir)
        refreshStats()
        emitChange()
    }

    private fun isWordLike(ru: String): Boolean =
        ru.split(Regex("[^А-Яа-яЁё]+")).any { it.length >= 2 }

    /** 可练习词书 = 用户导入的词库包。 */
    fun practiceBooks(): List<WordBook> = _vocabBooks.value

    /** 查词 / 搜索用的全部来源：词库包 + 教材课文派生词（已滤掉单字母）。 */
    fun lookupBooks(): List<WordBook> =
        _vocabBooks.value + _lessons.value.mapNotNull { l ->
            val entries = l.segments
                .map { WordEntry(it.ru, it.zh, it.audio) }
                .filter { isWordLike(it.ru) }
            if (entries.isEmpty()) null else
                WordBook(
                    id = "lesson:${l.n}",
                    name = "урок ${l.n} · 第${l.n}课",
                    kind = BookKind.LESSON,
                    entries = entries
                )
        }

    // ---------- 词库 ----------

    fun importBookText(text: String, name: String? = null): String? {
        val r = BookStore.importBookText(dir, text, name)
        reload()
        return r
    }

    fun downloadBook(url: String, name: String? = null): String? {
        val r = BookStore.downloadBook(dir, url, name)
        reload()
        return r
    }

    fun deleteBook(id: String) {
        BookStore.deleteBook(dir, id)
        reload()
    }

    // ---------- 教材 ----------

    fun importLessonsText(text: String): Int {
        val n = LessonStore.importLessonsText(dir, text)
        reload()
        return n
    }

    fun importLessonZip(zipPath: String): Int {
        val n = LessonStore.importZip(dir, zipPath)
        reload()
        return n
    }

    fun deleteLesson(n: Int) {
        LessonStore.deleteLesson(dir, n)
        reload()
    }

    // ---------- 查询 ----------

    fun lookup(word: String): WordEntry? {
        val w = word.trim().lowercase()
        if (w.isEmpty()) return null
        for (b in lookupBooks()) {
            val hit = b.entries.firstOrNull { it.ru.lowercase() == w }
            if (hit != null) return hit
        }
        return null
    }

    fun search(q: String, limit: Int = 80): List<WordEntry> {
        val w = q.trim().lowercase()
        if (w.isEmpty()) return emptyList()
        val seen = HashSet<String>()
        val all = ArrayList<WordEntry>()
        for (b in lookupBooks()) for (e in b.entries) {
            if (seen.add(e.ru.lowercase())) all.add(e)
        }
        return all.filter { it.ru.lowercase().contains(w) || it.zh.contains(w) }
            .sortedWith(compareBy({ !it.ru.lowercase().startsWith(w) }, { it.ru.length }))
            .take(limit)
    }

    // ---------- 掌握度 ----------

    fun masteryOf(ru: String): Mastery = ProgressStore.of(_progress.value, ru)

    fun recordAnswer(ru: String, correct: Boolean) {
        val next = ProgressStore.answer(_progress.value, ru, correct)
        val map = _progress.value.toMutableMap()
        map[ru.trim().lowercase()] = next
        ProgressStore.save(dir, map)
        _progress.value = map
        prefs.markStudied()
        refreshStats()
        emitChange()
    }

    fun resetProgress() {
        ProgressStore.clear(dir)
        _progress.value = emptyMap()
        refreshStats()
        emitChange()
    }

    /** 今日待复习 + 新词，按「到期优先、其次未学过」排序。 */
    fun reviewQueue(limit: Int = 40): List<WordEntry> {
        val now = System.currentTimeMillis()
        val prog = _progress.value
        val all = LinkedHashMap<String, WordEntry>()
        for (b in practiceBooks()) for (e in b.entries) {
            all.putIfAbsent(e.ru.lowercase(), e)
        }
        val due = all.values.filter { (prog[it.ru.lowercase()]?.due ?: 0L) <= now }
        return (due.shuffled() + all.values.filter { !prog.containsKey(it.ru.lowercase()) }.shuffled())
            .take(limit)
    }

    /** 桌面 / 首页用的「每日一词」。 */
    fun dailyWord(): Pair<String, String> {
        val slot = System.currentTimeMillis() / 86_400_000L + prefs.wordOffset()
        val all = practiceBooks().flatMap { it.entries }.distinctBy { it.ru.lowercase() }
        return if (all.isNotEmpty()) {
            val e = all[indexFor(slot, all.size)]
            e.ru to e.zh.ifBlank { "（暂无释义）" }
        } else RUSSIAN_QUOTES[indexFor(slot, RUSSIAN_QUOTES.size)]
    }

    private fun indexFor(slot: Long, size: Int): Int =
        Math.floorMod(slot, size.toLong()).toInt()

    fun refreshStats() {
        val books = practiceBooks()
        val all = LinkedHashMap<String, WordEntry>()
        for (b in books) for (e in b.entries) all.putIfAbsent(e.ru.lowercase(), e)
        val prog = _progress.value
        val now = System.currentTimeMillis()
        val (c, t) = prefs.spelling()
        _stats.value = StudyStats(
            totalWords = all.size,
            mastered = prog.count { it.value.level >= 4 },
            dueToday = all.entries.count { (prog[it.key]?.due ?: 0L) <= now },
            learnedToday = prefs.learnedToday(),
            streak = prefs.streak(),
            dailyGoal = prefs.dailyGoal(),
            quizBest = prefs.quizBest(),
            spellingAcc = if (t == 0) 0 else c * 100 / t
        )
    }
}
