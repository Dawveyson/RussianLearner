package com.example.rulearn.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.rulearn.data.Segment

/**
 * 统一播放器：assets 相对路径 / 本地绝对路径 / http(s) URL 都能放。
 * 支持整轨 + 时间戳的切段播放（start/end 秒）。
 */
class AudioPlayer(private val context: Context) {

    private var mp: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopTask: Runnable? = null

    var onComplete: (() -> Unit)? = null

    init {
        PlaybackBus.register(this)
    }

    /** 播放一段课文。segment.audio 为空时回落到 lessonAudio 并按 start/end 定位。 */
    fun playSegment(seg: Segment, lessonAudio: String = "") {
        val hasOwn = seg.audio.isNotBlank()
        val source = if (hasOwn) seg.audio else lessonAudio
        // 没有音源时也要回调，否则调用方的播放状态会一直停在「播放中」
        if (source.isBlank()) {
            onComplete?.invoke()
            return
        }
        val startMs = if (hasOwn) 0 else (seg.start * 1000).toInt()
        val endMs = if (hasOwn) -1 else (seg.end * 1000).toInt().takeIf { it > startMs } ?: -1
        play(source, startMs, endMs)
    }

    fun play(source: String, startMs: Int = 0, endMs: Int = -1) {
        release()
        // release() 会注销，这里重新登记；并让随身听和其它页面播放器让路，避免双响
        PlaybackBus.register(this)
        PlaybackBus.onLocalPlay(this)
        stopTask?.let { handler.removeCallbacks(it) }
        stopTask = null
        val player = MediaPlayer()
        try {
            applySource(player, source)
            player.setOnCompletionListener {
                cancelStopTask()
                onComplete?.invoke()
            }
            // 出错时也要走完结回调：否则连播中断，且调用方的 playing 状态永远卡在 true。
            player.setOnErrorListener { _, _, _ ->
                cancelStopTask()
                runCatching { player.release() }
                if (mp === player) mp = null
                onComplete?.invoke()
                true
            }
            // 用 prepareAsync 而非同步 prepare：网络/较大音频若用同步 prepare 会阻塞主线程，
            // 轻则卡顿、重则触发 ANR 被系统杀掉（表现为「点击后闪退」）。真正 start 推迟到 onPrepared。
            player.setOnPreparedListener {
                runCatching {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    if (startMs > 0) it.seekTo(startMs)
                    it.start()
                }
                mp = player
                if (endMs > startMs) {
                    val task = Runnable {
                        if (mp === player) {
                            runCatching { player.pause() }
                            onComplete?.invoke()
                        }
                    }
                    stopTask = task
                    handler.postDelayed(task, (endMs - startMs).toLong())
                }
            }
            player.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { player.release() }
            mp = null
            onComplete?.invoke()
        }
    }

    var speed: Float = 1f
        set(value) {
            field = value
            mp?.let {
                if (it.isPlaying) {
                    runCatching { it.playbackParams = it.playbackParams.setSpeed(value) }
                }
            }
        }

    fun pause() {
        cancelStopTask()
        runCatching { mp?.pause() }
    }

    fun resume() {
        runCatching { mp?.start() }
    }

    fun release() {
        cancelStopTask()
        PlaybackBus.unregister(this)
        mp?.let { runCatching { it.release() } }
        mp = null
    }

    private fun cancelStopTask() {
        stopTask?.let { handler.removeCallbacks(it) }
        stopTask = null
    }

    private fun applySource(player: MediaPlayer, source: String) {
        when {
            source.startsWith("http://") || source.startsWith("https://") -> player.setDataSource(source)
            source.startsWith("/") || source.startsWith("file://") ->
                player.setDataSource(source.removePrefix("file://"))
            else -> {
                // 先按 assets 解析，失败再按本地文件解析
                val afd = runCatching { context.assets.openFd(source) }.getOrNull()
                if (afd != null) {
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    runCatching { afd.close() }
                } else {
                    player.setDataSource(source)
                }
            }
        }
    }
}
