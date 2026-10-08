package com.example.rulearn.core

import android.content.Context

/** 轻量偏好存储。所有键集中在此，避免散落各处。 */
object Prefs {
    private const val NAME = "rulearn_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ---- 每日目标 ----
    fun dailyGoal(ctx: Context): Int = sp(ctx).getInt("daily_goal", 20)
    fun setDailyGoal(ctx: Context, v: Int) = sp(ctx).edit().putInt("daily_goal", v.coerceIn(5, 200)).apply()

    // ---- 打卡连续天数 ----
    fun streak(ctx: Context): Int = sp(ctx).getInt("streak", 0)
    fun lastStudyDay(ctx: Context): String = sp(ctx).getString("last_study_day", "") ?: ""

    /**
     * 每日小组件的内容偏移。
     * 每日内容按「天 % 总数」取，用户点「换一个」时 +1，这样当天也能手动跳过不想看的句子。
     */
    fun wordOffset(ctx: Context): Int = sp(ctx).getInt("word_offset", 0)
    fun bumpWordOffset(ctx: Context): Int {
        val v = wordOffset(ctx) + 1
        sp(ctx).edit().putInt("word_offset", v).apply()
        return v
    }

    /** 本地日期 yyyy-MM-dd，用于打卡与「今日已学」。 */
    fun todayKey(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date())

    /** 今日已学词条数（跨天自动归零）。 */
    fun learnedToday(ctx: Context): Int =
        if (lastStudyDay(ctx) == todayKey()) sp(ctx).getInt("learned_today", 0) else 0

    /** 记录一次学习：维护今日计数与连续打卡天数。 */
    fun markStudied(ctx: Context) {
        val today = todayKey()
        val last = lastStudyDay(ctx)
        if (last != today) {
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(System.currentTimeMillis() - 86_400_000L))
            val nextStreak = if (last == yesterday) streak(ctx) + 1 else 1
            sp(ctx).edit()
                .putInt("learned_today", if (last == today) sp(ctx).getInt("learned_today", 0) + 1 else 1)
                .putInt("streak", nextStreak)
                .putString("last_study_day", today)
                .apply()
        } else {
            sp(ctx).edit().putInt("learned_today", sp(ctx).getInt("learned_today", 0) + 1).apply()
        }
    }

    // ---- 默写累计 ----
    fun spelling(ctx: Context): Pair<Int, Int> =
        sp(ctx).getInt("sp_correct", 0) to sp(ctx).getInt("sp_total", 0)

    fun recordSpelling(ctx: Context, correct: Boolean) {
        val (c, t) = spelling(ctx)
        sp(ctx).edit()
            .putInt("sp_correct", c + if (correct) 1 else 0)
            .putInt("sp_total", t + 1)
            .apply()
    }

    // ---- 刷题最佳 ----
    fun quizBest(ctx: Context): Int = sp(ctx).getInt("quiz_best", 0)
    fun setQuizBest(ctx: Context, v: Int) {
        if (v > sp(ctx).getInt("quiz_best", 0)) sp(ctx).edit().putInt("quiz_best", v).apply()
    }

    // ---- 有道智云密钥 ----
    fun apiKey(ctx: Context): String = sp(ctx).getString("yd_appkey", "") ?: ""
    fun setApiKey(ctx: Context, v: String) = sp(ctx).edit().putString("yd_appkey", v.trim()).apply()
    fun apiSecret(ctx: Context): String = sp(ctx).getString("yd_secret", "") ?: ""
    fun setApiSecret(ctx: Context, v: String) = sp(ctx).edit().putString("yd_secret", v.trim()).apply()

    // ---- 发音：优先网络 ----
    fun preferNetworkTts(ctx: Context): Boolean = sp(ctx).getBoolean("prefer_net_tts", true)
    fun setPreferNetworkTts(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("prefer_net_tts", v).apply()

    // ---- 当前选中的练习词书 ----
    fun activeBook(ctx: Context): String = sp(ctx).getString("active_book", "") ?: ""
    fun setActiveBook(ctx: Context, v: String) = sp(ctx).edit().putString("active_book", v).apply()

    // ---- 开始学习：自动朗读 ----
    fun autoPlayAudio(ctx: Context): Boolean = sp(ctx).getBoolean("auto_play_audio", true)
    fun setAutoPlayAudio(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("auto_play_audio", v).apply()

    // ---- 开始学习：单次学习/复习单词数 ----
    fun sessionSize(ctx: Context): Int = sp(ctx).getInt("session_size", 10).coerceIn(5, 20)
    fun setSessionSize(ctx: Context, v: Int) = sp(ctx).edit().putInt("session_size", v.coerceIn(5, 20)).apply()

    // ---- 离线语音包 ----
    fun ttsOfflineFirst(ctx: Context): Boolean = sp(ctx).getBoolean("tts_offline_first", false)
    fun setTtsOfflineFirst(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("tts_offline_first", v).apply()

    /** 自动下载离线发音包：播放/学习时后台静默缓存缺失的词，开启时还会拉取整包。 */
    fun autoDownloadTts(ctx: Context): Boolean = sp(ctx).getBoolean("auto_download_tts", true)
    fun setAutoDownloadTts(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("auto_download_tts", v).apply()

    // ---- 每天第一次学习前，是否已提示过「先复习旧单词」 ----
    fun dailyReviewPromptDay(ctx: Context): String = sp(ctx).getString("daily_review_prompt_day", "") ?: ""
    fun setDailyReviewPromptDay(ctx: Context, v: String) = sp(ctx).edit().putString("daily_review_prompt_day", v).apply()

}
