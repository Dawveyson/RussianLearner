package com.example.rulearn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.rulearn.MainActivity
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.R

/**
 * 学习提醒小组件。
 *
 * 显示今日学习进度、到期复习量、连续天数；点任意位置进入 App。
 * 数据来自 [AppRepository]，用户在 App 里每答一题都会触发一次刷新。
 */
class StudyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ensureReady(context)
        val stats = AppRepository.stats.value
        val views = buildViews(context, stats.learnedToday, stats.dailyGoal, stats.dueToday, stats.streak)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    companion object {

        private fun buildViews(ctx: Context, learned: Int, goal: Int, due: Int, streak: Int): RemoteViews {
            val done = goal > 0 && learned >= goal
            return RemoteViews(ctx.packageName, R.layout.widget_study).apply {
                setTextViewText(R.id.streak, if (streak > 0) "连续 $streak 天" else "")
                if (done) {
                    setTextViewText(R.id.progress, "今天的目标完成啦 $learned/$goal")
                    setTextViewText(R.id.due, "明天见")
                } else {
                    setTextViewText(R.id.progress, "今日进度 $learned / $goal")
                    setTextViewText(
                        R.id.due,
                        if (due > 0) "还有 $due 个词到期复习" else "今天没有到期的复习任务"
                    )
                }
                setOnClickPendingIntent(android.R.id.background, openApp(ctx))
            }
        }

        /** 点击整个卡片打开 App。 */
        private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** 由 [WidgetRefresher] 调用：把桌面上所有实例刷新一遍。 */
        fun refreshAll(ctx: Context) {
            ensureReady(ctx)
            val stats = AppRepository.stats.value
            val views = buildViews(ctx, stats.learnedToday, stats.dailyGoal, stats.dueToday, stats.streak)
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, StudyWidgetProvider::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
        }
    }
}

/**
 * 每日一词 / 每日一句。
 *
 * 优先展示用户自己词库里的词条；还没导入词库时显示内置的一组俄语短句。
 * 「换一个」会把内容偏移 +1，相当于手动翻页。
 */
class DailyWordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    /** 收到「换一个」广播后刷新自己。 */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT) {
            Prefs.bumpWordOffset(context)
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_NEXT = "com.example.rulearn.widget.ACTION_NEXT_WORD"

        private fun buildViews(ctx: Context): RemoteViews {
            ensureReady(ctx)
            val (ru, zh) = AppRepository.dailyWord(ctx)
            val fromBook = AppRepository.vocabBooks.value.isNotEmpty() ||
                AppRepository.lessons.value.isNotEmpty()
            return RemoteViews(ctx.packageName, R.layout.widget_daily).apply {
                setTextViewText(R.id.label, if (fromBook) "每日一词" else "每日一句")
                setTextViewText(R.id.ru, ru)
                setTextViewText(R.id.zh, zh)
                setOnClickPendingIntent(android.R.id.background, openApp(ctx))
                setOnClickPendingIntent(R.id.next, nextIntent(ctx))
            }
        }

        private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
            ctx,
            1,
            Intent(ctx, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun nextIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
            ctx,
            2,
            Intent(ctx, DailyWordWidgetProvider::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** 由 [WidgetRefresher] 调用。 */
        fun refreshAll(ctx: Context) {
            val views = buildViews(ctx)
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, DailyWordWidgetProvider::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
        }
    }
}

/** 小组件跑在 App 自己的进程里，但有时候数据还没加载，这里兜一下。 */
internal fun ensureReady(context: Context) {
    if (!AppRepository.ready) AppRepository.init(context)
}
