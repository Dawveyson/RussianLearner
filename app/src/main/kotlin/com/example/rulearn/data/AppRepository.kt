package com.example.rulearn.data

import android.content.Context
import android.net.Uri
import com.example.rulearn.core.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全 App 单一数据源。所有页面只读这里的状态流，任何写操作都通过这里落地并回流，
 * 因此导入 / 删除 / 练习后 UI 会立即刷新。
 */
object AppRepository {

    private val _vocabBooks = MutableStateFlow<List<WordBook>>(emptyList())
    val vocabBooks: StateFlow<List<WordBook>> = _vocabBooks.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, Mastery>>(emptyMap())
    val progress: StateFlow<Map<String, Mastery>> = _progress.asStateFlow()

    private val _stats = MutableStateFlow(StudyStats())
    val stats: StateFlow<StudyStats> = _stats.asStateFlow()

    private lateinit var app: Context

    /** 桌面小组件跑在独立进程，进来时可能还没初始化过，用这个判断是否需要 init。 */
    val ready: Boolean get() = ::app.isInitialized

    /**
     * 数据变更钩子。App 启动时把它接成「刷新桌面小组件」，
     * 这样答完题、导入词库、删教材之后桌面上的数据会立刻跟上。
     * 放在这里是为了避免 data 层反向依赖 widget 层。
     */
    var onDataChanged: ((Context) -> Unit)? = null

    private fun emitChange() {
        if (::app.isInitialized) runCatching { onDataChanged?.invoke(app) }
    }

    fun init(ctx: Context) {
        app = ctx.applicationContext
        reload()
    }

    fun reload() {
        _vocabBooks.value = BookStore.load(app)
        _lessons.value = LessonStore.load(app)
        _progress.value = ProgressStore.load(app)
        refreshStats()
        emitChange()
    }

    /**
     * 是否算一个「词」：至少含一个 2 字母以上的西里尔词。
     * 用来滤掉语音课里的单字母（А / О / У 以及 "А, О, А" 这类），
     * 否则它们会被当成单词混进拼写、测验和复习。
     */
    private fun isWordLike(ru: String): Boolean =
        ru.split(Regex("[^А-Яа-яЁё]+")).any { it.length >= 2 }

    /**
     * 可练习词书 = 用户导入的【词库包】（单词拼写资源包）。
     * 教材课文（课本资源包）只用于「随身听」和「查单词」，不再混入拼写 / 测验 / 复习，
     * 否则语音课里的单字母、音节（А / НУ / ДО）会被当成单词练。
     */
    fun practiceBooks(): List<WordBook> = _vocabBooks.value

    /**
     * 查词 / 搜索用的全部来源：词库包 + 教材课文派生词（已滤掉单字母）。
     * 课文里的词也能被查到，但不会进拼写练习。
     */
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

    fun importBook(uri: Uri, name: String? = null): String? {
        val r = BookStore.importBook(app, uri, name)
        reload()
        return r
    }

    fun downloadBook(url: String, name: String? = null): String? {
        val r = BookStore.downloadBook(app, url, name)
        reload()
        return r
    }

    fun deleteBook(id: String) {
        BookStore.delete(app, id)
        reload()
    }

    // ---------- 教材 ----------

    fun importLessons(uri: Uri): Int {
        val n = LessonStore.importFrom(app, uri)
        reload()
        return n
    }

    fun importLessonZip(uri: Uri): Int {
        val n = LessonStore.importZip(app, uri)
        reload()
        return n
    }

    fun deleteLesson(n: Int) {
        LessonStore.delete(app, n)
        reload()
    }

    // ---------- 查询 ----------

    /** 离线查词：先在用户词库找，再在课文里找。 */
    fun lookup(word: String): WordEntry? {
        val w = word.trim().lowercase()
        if (w.isEmpty()) return null
        for (b in lookupBooks()) {
            val hit = b.entries.firstOrNull { it.ru.lowercase() == w }
            if (hit != null) return hit
        }
        return null
    }

    /** 模糊搜索：前缀优先，最多 limit 条。 */
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
        ProgressStore.save(app, map)
        _progress.value = map
        Prefs.markStudied(app)
        refreshStats()
        emitChange()
    }

    fun resetProgress() {
        ProgressStore.clear(app)
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

    /**
     * 桌面小组件用的「每日一词」。
     * 优先从用户自己的词库里取（按天轮换，保证同一天多次刷新结果一致），
     * 用户还没导入词库时回落到内置的一组俄语短句，让小组件不至于空白。
     */
    fun dailyWord(ctx: Context): Pair<String, String> {
        val slot = System.currentTimeMillis() / 86_400_000L + Prefs.wordOffset(ctx)
        val all = practiceBooks().flatMap { it.entries }.distinctBy { it.ru.lowercase() }
        if (all.isNotEmpty()) {
            val e = all[indexFor(slot, all.size)]
            return e.ru to e.zh.ifBlank { "（暂无释义）" }
        }
        return RUSSIAN_QUOTES[indexFor(slot, RUSSIAN_QUOTES.size)]
    }

    /**
     * 把天数偏移映射成合法下标。
     * 必须用 floorMod：Kotlin 的 % 对负数返回负值，直接拿去索引会抛
     * IndexOutOfBoundsException（偏移量被「换一个」减成负数时就会触发）。
     */
    private fun indexFor(slot: Long, size: Int): Int =
        Math.floorMod(slot, size.toLong()).toInt()

    fun refreshStats() {
        val books = practiceBooks()
        val all = LinkedHashMap<String, WordEntry>()
        for (b in books) for (e in b.entries) all.putIfAbsent(e.ru.lowercase(), e)
        val prog = _progress.value
        val now = System.currentTimeMillis()
        val (c, t) = Prefs.spelling(app)
        _stats.value = StudyStats(
            totalWords = all.size,
            mastered = prog.count { it.value.level >= 4 },
            dueToday = all.entries.count { (prog[it.key]?.due ?: 0L) <= now },
            learnedToday = Prefs.learnedToday(app),
            streak = Prefs.streak(app),
            dailyGoal = Prefs.dailyGoal(app),
            quizBest = Prefs.quizBest(app),
            spellingAcc = if (t == 0) 0 else c * 100 / t
        )
    }
}
