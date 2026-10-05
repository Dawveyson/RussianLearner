package com.example.rulearn.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 偏好封装，对应原 Android 的 Prefs（打卡、默写、刷题、密钥、设置等）。
 * 全部落在传入的 KeyValueStore 上，Android / 桌面通用。
 */
class AppPrefs(private val store: KeyValueStore) {

    fun dailyGoal(): Int = store.getInt("daily_goal", 20)
    fun setDailyGoal(v: Int) = store.putInt("daily_goal", v.coerceIn(5, 200))

    fun streak(): Int = store.getInt("streak", 0)
    fun lastStudyDay(): String = store.getString("last_study_day", "")

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun learnedToday(): Int =
        if (lastStudyDay() == todayKey()) store.getInt("learned_today", 0) else 0

    /** 记录一次学习：维护今日计数与连续打卡天数。 */
    fun markStudied() {
        val today = todayKey()
        val last = lastStudyDay()
        if (last != today) {
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(System.currentTimeMillis() - 86_400_000L))
            val nextStreak = if (last == yesterday) streak() + 1 else 1
            store.putInt("learned_today", if (last == today) learnedToday() + 1 else 1)
            store.putInt("streak", nextStreak)
            store.putString("last_study_day", today)
        } else {
            store.putInt("learned_today", learnedToday() + 1)
        }
    }

    fun wordOffset(): Int = store.getInt("word_offset", 0)
    fun bumpWordOffset(): Int {
        val v = wordOffset() + 1
        store.putInt("word_offset", v)
        return v
    }

    fun spelling(): Pair<Int, Int> =
        store.getInt("sp_correct", 0) to store.getInt("sp_total", 0)

    fun recordSpelling(correct: Boolean) {
        val (c, t) = spelling()
        store.putInt("sp_correct", c + if (correct) 1 else 0)
        store.putInt("sp_total", t + 1)
    }

    fun quizBest(): Int = store.getInt("quiz_best", 0)
    fun setQuizBest(v: Int) {
        if (v > quizBest()) store.putInt("quiz_best", v)
    }

    fun apiKey(): String = store.getString("yd_appkey", "")
    fun setApiKey(v: String) = store.putString("yd_appkey", v.trim())
    fun apiSecret(): String = store.getString("yd_secret", "")
    fun setApiSecret(v: String) = store.putString("yd_secret", v.trim())

    fun preferNetworkTts(): Boolean = store.getBoolean("prefer_net_tts", true)
    fun setPreferNetworkTts(v: Boolean) = store.putBoolean("prefer_net_tts", v)

    fun activeBook(): String = store.getString("active_book", "")
    fun setActiveBook(v: String) = store.putString("active_book", v)
}
