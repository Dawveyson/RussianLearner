package com.example.rulearn.quiz

import com.example.rulearn.WordBook
import com.example.rulearn.WordEntry

/** 归一化：小写、去两端空白、压缩内部空白，用于拼写/答案比较。 */
fun norm(s: String): String =
    s.trim().lowercase().replace(Regex("\\s+"), " ")

/** 测验题型。 */
enum class QuizMode { ZH2RU, RU2ZH, SPELL }

/**
 * 一道题：题干、四个选项（SPELL 模式下 options 为空）、正确答案下标、来源词条。
 */
data class Question(
    val mode: QuizMode,
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val entry: WordEntry
)

/**
 * 从练习词书构造一组题目。题目数为 count，mode 均匀分配。
 * choices 为干扰项数量（默认 4 选 1）。
 */
fun buildQuestions(books: List<WordBook>, count: Int, choices: Int = 4, seed: Long = 12345L): List<Question> {
    val practice = books.flatMap { it.entries }.distinctBy { it.ru.lowercase() }
    if (practice.isEmpty()) return emptyList()
    val rng = kotlin.random.Random(seed)
    val picked = practice.shuffled(rng).take(count)
    val modes = QuizMode.values()
    return picked.mapIndexed { i, e ->
        val mode = modes[i % modes.size]
        when (mode) {
            QuizMode.SPELL -> Question(mode, e.zh, emptyList(), 0, e)
            else -> {
                val answerIsRu = mode == QuizMode.RU2ZH
                val correct = if (answerIsRu) e.ru else e.zh
                val pool = (practice - e).shuffled(rng)
                val distractors = pool.take(choices - 1).map { if (answerIsRu) it.ru else it.zh }
                val opts = (listOf(correct) + distractors).shuffled(rng)
                Question(mode, if (answerIsRu) e.ru else e.zh, opts, opts.indexOf(correct), e)
            }
        }
    }
}
