package com.example.rulearn.data

/** 一个词条。img / note 为可选扩展字段，导入时留空即可。 */
data class WordEntry(
    val ru: String,
    val zh: String,
    val audio: String = "",
    val img: String = "",
    val note: String = ""
)

/** 词书来源：用户词库或教材课文。 */
enum class BookKind { VOCAB, LESSON }

/** 一本可练习的词书。 */
data class WordBook(
    val id: String,
    val name: String,
    val kind: BookKind,
    val entries: List<WordEntry>
) {
    val hasAudio: Boolean get() = entries.any { it.audio.isNotBlank() }
}

/**
 * 课文的一段。
 * - audio：本段独立音频，可为 assets 相对路径、本地绝对路径或 http(s) URL。
 * - start/end：当只有整课音频时，用这两个时间戳做精确定位与切段。
 * - dur：时长（秒），可由 end-start 推导，仅作兜底。
 */
data class Segment(
    val i: Int,
    val ru: String,
    val zh: String,
    val dur: Double = 0.0,
    val audio: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0
)

/** 一课（教材）。audio 为整课音频，供未切分片段的段落使用。 */
data class Lesson(
    val n: Int,
    val title: String,
    val segments: List<Segment>,
    val audio: String = ""
)

/** 单个词的掌握度（轻量 SRS）。level 0..5，due 为到期毫秒时间戳。 */
data class Mastery(
    val level: Int = 0,
    val due: Long = 0L,
    val seen: Int = 0,
    val ok: Int = 0,
    val bad: Int = 0
)

/** 首页用的学习概览。 */
data class StudyStats(
    val totalWords: Int = 0,
    val mastered: Int = 0,
    val dueToday: Int = 0,
    val learnedToday: Int = 0,
    val streak: Int = 0,
    val dailyGoal: Int = 20,
    val quizBest: Int = 0,
    val spellingAcc: Int = 0
)

/** 俄文字母表条目。 */
data class LetterInfo(
    val upper: String,
    val lower: String,
    val sound: String,
    val sampleRu: String,
    val sampleZh: String
)
