package com.example.rulearn.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.rulearn.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 后台下载离线语音包的前台服务。
 *
 * 退出「设置 → 离线语音包」页面后，若还有词没缓存且开启了自动下载，就启动它，
 * 在后台继续把缺失的词下载完，并通过一条原生进度通知（Android 上的“Live Activity”等价物，
 * 出现在通知栏与锁屏）实时显示进度。下载完成显示可滑走的“完成”通知后停止。
 */
class TtsPackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须先 startForeground，否则 Android 12+ 会直接 ANR / 崩溃
        val notif = buildNotification(0, 1, "正在后台下载离线语音包…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需显式声明前台服务类型（对应 manifest 的 dataSync）
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        scope.launch {
            try {
                _running.value = true
                val words = AppRepository.practiceBooks().flatMap { it.entries }
                    .map { it.ru }.distinct()
                // 只下载本地缺失的词，避免重复下载
                val missing = words.filter { !TtsPack.has(this@TtsPackService, it) }
                val total = missing.size
                _progress.value = 0 to total
                missing.forEachIndexed { i, w ->
                    runCatching { TtsPack.ensure(this@TtsPackService, w) }
                    val done = i + 1
                    _progress.value = done to total
                    updateNotification(done, total)
                }
                updateNotification(total, total, "离线语音包下载完成 ✓")
            } finally {
                _running.value = false
                val t = _progress.value.second
                if (t > 0) {
                    // 完成通知：不再常驻，可手动滑走；6 秒后自动消失
                    updateNotification(t, t, "离线语音包下载完成 ✓", ongoing = false)
                    // 脱钩为普通通知（保留可见），而非随服务停止被立即移除
                    stopForeground(STOP_FOREGROUND_DETACH)
                    Handler(Looper.getMainLooper()).postDelayed({
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .cancel(NOTIF_ID)
                    }, 6000)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "离线语音包下载", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "后台下载离线发音包的进度" }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    /**
     * 构建下载进度通知 —— Android 上对标 iOS Live Activity 的“实时活动”实现。
     * 直接走原生 NotificationManager / NotificationCompat API（非 Compose 页面），
     * 会出现在通知栏与锁屏，实时展示进度；锁屏可见、静默更新（不重复响铃/震动）。
     */
    private fun buildNotification(done: Int, total: Int, text: String, ongoing: Boolean = true): android.app.Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val detail = if (total > 0) "$text\n$done / $total" else text
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("离线语音包")
            .setContentText(if (total > 0) "$text $done/$total" else text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(total.coerceAtLeast(1), done, false)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .setOnlyAlertOnce(true)                              // 进度刷新不重复响铃/震动
            .setOngoing(ongoing)
        // Android 16+ 原生 Live Updates：ongoing 进度通知会被系统以锁屏实时活动
        // （类灵动岛的 chip）呈现。这里在 16+ 显式使用原生 CATEGORY_PROGRESS，低版本走 Compat 等价常量。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setCategory(android.app.Notification.CATEGORY_PROGRESS)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+：立即显示前台通知，不延迟
            builder.setForegroundServiceBehavior(android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun updateNotification(
        done: Int,
        total: Int,
        text: String = "正在后台下载离线语音包…",
        ongoing: Boolean = true
    ) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(done, total, text, ongoing))
    }

    override fun onDestroy() {
        started = false
        _running.value = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "tts_pack_channel"
        const val NOTIF_ID = 1002

        /** 当前是否正在后台下载（供设置页订阅，用来禁用按钮/显示进度）。 */
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        /** 下载进度 (已完成, 总数)，总数只统计本地缺失的词。 */
        private val _progress = MutableStateFlow(0 to 0)
        val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

        /** 防止重复拉起服务（页面多处可能调用 start）。 */
        @Volatile
        private var started = false

        fun start(ctx: Context) {
            if (started) return
            started = true
            runCatching {
                val i = Intent(ctx, TtsPackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            }.onFailure { started = false }
        }
    }
}
