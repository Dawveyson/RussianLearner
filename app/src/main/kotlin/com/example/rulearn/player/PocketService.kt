package com.example.rulearn.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.rulearn.MainActivity
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.Segment

/**
 * 随身听：前台服务连续播放课文，熄屏 / 后台也能听。
 * 支持「整轨 + 时间戳」与「逐段音频」两种教材形态。
 */
class PocketService : Service() {

    private data class PlayItem(
        val source: String,
        val startMs: Int,
        val endMs: Int,
        val label: String,
        val ru: String,
        val zh: String = ""
    )

    private var mp: MediaPlayer? = null

    /**
     * 连续播放失败次数。
     * 失败会顺带跳到下一段，若整份教材的音频都缺失/损坏，就会一路递归跳完整列表，
     * 段数很大时可能把栈打爆。这里设个上限：连续失败超过 3 次就停下并提示。
     */
    private var failStreak = 0
    private var session: MediaSession? = null
    private var playlist: List<PlayItem> = emptyList()
    private var pos = 0
    private val handler = Handler(Looper.getMainLooper())
    private var stopTask: Runnable? = null
    private var ticker: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        PlaybackBus.attachPocket(this)
        session = MediaSession(this, "RuLearnPocket").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = prev()
                override fun onStop() = shutdown()
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                buildPlaylist(intent.getIntExtra(EXTRA_LESSON, 0), intent.getBooleanExtra(EXTRA_RANDOM, false))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIF_ID, buildNotification())
                }
                pos = 0
                playCurrent()
            }
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> next()
            ACTION_PREV -> prev()
            ACTION_STOP -> shutdown()
            ACTION_SEEK_INDEX -> {
                val i = intent.getIntExtra(EXTRA_INDEX, -1)
                if (i in playlist.indices) { pos = i; playCurrent() }
            }
            ACTION_SEEK_MS -> {
                val ms = intent.getIntExtra(EXTRA_MS, -1)
                seekWithin(ms)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildPlaylist(lesson: Int, random: Boolean) {
        val items = ArrayList<PlayItem>()
        val lessons = AppRepository.lessons.value
        val scope = if (lesson <= 0) lessons else lessons.filter { it.n == lesson }
        for (l in scope) {
            for (s in l.segments) {
                val own = s.audio.isNotBlank()
                val source = if (own) s.audio else l.audio
                if (source.isBlank()) continue
                val startMs = if (own) 0 else (s.start * 1000).toInt()
                val endMs = if (own) -1 else (s.end * 1000).toInt().takeIf { it > startMs } ?: -1
                items.add(PlayItem(source, startMs, endMs, "урок ${l.n} · ${s.ru}", s.ru, s.zh))
            }
        }
        playlist = if (random) items.shuffled() else items
        PocketState.total = playlist.size
        PocketState.lines = playlist.map { it.ru }
        PocketState.linesZh = playlist.map { it.zh }
        PocketState.currentIndex = if (playlist.isEmpty()) -1 else 0
    }

    private fun playCurrent() {
        if (playlist.isEmpty()) return
        val item = playlist[pos]
        try {
            releasePlayer()
            mp = MediaPlayer().apply {
                applySource(this, item.source)
                setOnCompletionListener { next(auto = true) }
                prepare()
                if (item.startMs > 0) seekTo(item.startMs)
                start()
            }
            // 随身听要发声：让页面里的播放器让路，避免「教材和随身听同时响」
            failStreak = 0
            PlaybackBus.onPocketPlay()
            PocketState.isPlaying = true
            PocketState.currentIndex = pos
            PocketState.label = item.label
            PocketState.durationMs = if (item.endMs > item.startMs) item.endMs - item.startMs
            else runCatching { mp?.duration ?: 0 }.getOrDefault(0)
            PocketState.positionMs = 0
            scheduleEnd(item)
            startTicker()
            refresh()
        } catch (e: Exception) {
            e.printStackTrace()
            failStreak++
            if (failStreak >= 3) {
                // 连续这么多段都放不出来，多半是音频路径失效了，停在这里比重试更有意义
                releasePlayer()
                PocketState.isPlaying = false
                PocketState.label = "音频无法播放，请检查音频文件"
                refresh()
                return
            }
            next(auto = true)
        }
    }

    /** 在当前段内拖动到 ms（相对该段起点）。 */
    private fun seekWithin(ms: Int) {
        if (ms < 0) return
        val item = playlist.getOrNull(pos) ?: return
        val target = (if (item.startMs > 0) item.startMs else 0) + ms
        runCatching { mp?.seekTo(target) }
        PocketState.positionMs = ms
        // 拖动后重新安排段结束，避免半途被旧计时器切走
        scheduleEnd(item)
    }

    private fun startTicker() {
        stopTicker()
        ticker = object : Runnable {
            override fun run() {
                val item = playlist.getOrNull(pos) ?: return
                val cur = runCatching { mp?.currentPosition ?: 0 }.getOrDefault(0)
                PocketState.positionMs = if (item.startMs > 0) maxOf(0, cur - item.startMs) else cur
                PocketState.durationMs = if (item.endMs > item.startMs) item.endMs - item.startMs
                else runCatching { mp?.duration ?: 0 }.getOrDefault(0)
                handler.postDelayed(this, 200)
            }
        }
        handler.postDelayed(ticker!!, 200)
    }

    private fun stopTicker() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    private fun scheduleEnd(item: PlayItem) {
        cancelStopTask()
        if (item.endMs > item.startMs) {
            // 按「当前播放进度」算剩余时间，这样暂停后恢复也不会把这段播过头
            val cur = runCatching { mp?.currentPosition ?: item.startMs }.getOrDefault(item.startMs)
            val remain = item.endMs - cur
            if (remain <= 0) {
                next(auto = true)
                return
            }
            val task = Runnable { next(auto = true) }
            stopTask = task
            handler.postDelayed(task, remain.toLong())
        }
    }

    private fun cancelStopTask() {
        stopTask?.let { handler.removeCallbacks(it) }
        stopTask = null
    }

    private fun resume() {
        if (mp?.isPlaying == true) return
        if (mp == null && playlist.isNotEmpty()) { playCurrent(); return }
        runCatching { mp?.start() }
        PocketState.isPlaying = true
        PlaybackBus.onPocketPlay()
        startTicker()
        // 按当前进度重新安排本段结束，否则恢复后这一段会播过头
        playlist.getOrNull(pos)?.let { scheduleEnd(it) }
        refresh()
    }

    // internal：供 PlaybackBus 在页面播放器发声时把随身听暂停掉
    internal fun pause() {
        runCatching { mp?.pause() }
        PocketState.isPlaying = false
        stopTicker()
        // 关键：必须一并取消「本段结束自动跳下一段」的定时任务。
        // 以前没取消，任务照常触发 -> next(auto) -> playCurrent()，
        // 表现为「暂停了又自己接着播」。
        cancelStopTask()
        refresh()
    }

    private fun next(auto: Boolean = false) {
        stopTask?.let { handler.removeCallbacks(it) }
        stopTask = null
        if (playlist.isEmpty()) return
        if (pos + 1 < playlist.size) {
            pos++
            playCurrent()
        } else if (auto) {
            // 全部播完：释放播放器并把游标退回开头，下一次播放才是重播全列表。
            // 若保留 mp 与末尾 pos，再点播放会 resume 已结束的尾段，听不到声音也走不下去。
            releasePlayer()
            pos = 0
            PocketState.isPlaying = false
            PocketState.currentIndex = 0
            PocketState.label = playlist.firstOrNull()?.label ?: "未在播放"
            refresh()
        }
    }

    private fun prev() {
        if (playlist.isEmpty()) return
        if (pos - 1 >= 0) {
            pos--
            playCurrent()
        }
    }

    private fun shutdown() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseAll()
        stopSelf()
    }

    private fun refresh() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification())
        updateSession()
    }

    private fun updateSession() {
        val state = if (PocketState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        session?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private fun buildNotification(): Notification {
        val content = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val builder = notifBuilder()
            .setContentTitle("随身听")
            .setContentText(PocketState.label)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(content)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_previous, "上一个", pi(ACTION_PREV, 4))
            .addAction(
                if (PocketState.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (PocketState.isPlaying) "暂停" else "播放",
                if (PocketState.isPlaying) pi(ACTION_PAUSE, 2) else pi(ACTION_PLAY, 1)
            )
            .addAction(android.R.drawable.ic_media_next, "下一个", pi(ACTION_NEXT, 3))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", pi(ACTION_STOP, 5))
        builder.setStyle(
            Notification.MediaStyle()
                .setMediaSession(session?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        return builder.build()
    }

    private fun notifBuilder(): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)

    private fun pi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, PocketService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "随身听播放", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun applySource(player: MediaPlayer, source: String) {
        when {
            source.startsWith("http://") || source.startsWith("https://") -> player.setDataSource(source)
            source.startsWith("/") || source.startsWith("file://") -> {
                // 绝对路径（导入的本地音频，可能在 app 私有目录）：
                // 直接用路径会让 mediaserver 进程因无权访问私有目录而 EACCES，
                // 改为由 app 自己 open 出 fd 再交给播放器，避开权限问题。
                val path = source.removePrefix("file://")
                val f = java.io.File(path)
                if (f.exists()) {
                    val pfd = android.os.ParcelFileDescriptor.open(
                        f, android.os.ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    player.setDataSource(pfd.fileDescriptor)
                    runCatching { pfd.close() }
                } else {
                    player.setDataSource(path)
                }
            }
            else -> {
                val afd = runCatching { assets.openFd(source) }.getOrNull()
                if (afd != null) {
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    runCatching { afd.close() }
                } else {
                    player.setDataSource(source)
                }
            }
        }
    }

    private fun releasePlayer() {
        cancelStopTask()
        stopTicker()
        mp?.let { runCatching { it.release() } }
        mp = null
    }

    private fun releaseAll() {
        releasePlayer()
        session?.release()
        session = null
        PocketState.isPlaying = false
        PocketState.currentIndex = -1
        PocketState.label = "未在播放"
        PocketState.lines = emptyList()
        PocketState.linesZh = emptyList()
        PocketState.positionMs = 0
        PocketState.durationMs = 0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        PlaybackBus.detachPocket(this)
        releaseAll()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "pocket_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.example.rulearn.pocket.START"
        const val ACTION_PLAY = "com.example.rulearn.pocket.PLAY"
        const val ACTION_PAUSE = "com.example.rulearn.pocket.PAUSE"
        const val ACTION_NEXT = "com.example.rulearn.pocket.NEXT"
        const val ACTION_PREV = "com.example.rulearn.pocket.PREV"
        const val ACTION_STOP = "com.example.rulearn.pocket.STOP"
        const val ACTION_SEEK_INDEX = "com.example.rulearn.pocket.SEEK_INDEX"
        const val ACTION_SEEK_MS = "com.example.rulearn.pocket.SEEK_MS"
        const val EXTRA_LESSON = "lesson"
        const val EXTRA_RANDOM = "random"
        const val EXTRA_INDEX = "index"
        const val EXTRA_MS = "ms"

        fun start(context: Context, lesson: Int, random: Boolean) {
            launch(context, Intent(context, PocketService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LESSON, lesson)
                putExtra(EXTRA_RANDOM, random)
            })
        }

        fun send(context: Context, action: String, index: Int = -1, ms: Int = -1) {
            launch(context, Intent(context, PocketService::class.java).apply {
                this.action = action
                if (index >= 0) putExtra(EXTRA_INDEX, index)
                if (ms >= 0) putExtra(EXTRA_MS, ms)
            })
        }

        private fun launch(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
