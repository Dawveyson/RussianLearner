package com.example.rulearn.widget

import android.content.Context

/**
 * 桌面小组件的刷新入口。
 *
 * 由 App 启动时挂到 [com.example.rulearn.data.AppRepository.onDataChanged] 上，
 * 这样答题、导入词库、删教材之后桌面上的数据会立刻跟上。
 * 任何异常都吞掉——桌面没放小组件时刷新会失败，不能因此拖垮主流程。
 */
object WidgetRefresher {

    fun refresh(context: Context) {
        val appCtx = context.applicationContext
        runCatching { StudyWidgetProvider.refreshAll(appCtx) }
        runCatching { DailyWordWidgetProvider.refreshAll(appCtx) }
    }
}
