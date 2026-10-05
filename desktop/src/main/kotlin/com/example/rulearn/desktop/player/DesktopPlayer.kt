package com.example.rulearn.desktop.player

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * 桌面端音频播放。支持三类音源：
 *  - http(s) URL（在线 / 网络发音）
 *  - classpath 资源（内置字母/例词音频，如 "audio/letters/letter_01.mp3"）
 *  - 本地绝对路径（用户导入的 zip 音频书解压后的文件）
 * 用 mp3spi 解码 mp3；wav/无压缩格式原生支持。
 */
class DesktopPlayer {

    @Volatile private var current: SourceDataLine? = null
    @Volatile private var stopped = false
    @Volatile private var paused = false

    /** 播放一个音源，结束（或失败）后回调 onComplete。返回是否成功开始。 */
    fun play(source: String, onComplete: () -> Unit = {}): Boolean {
        stop()
        val stream = try {
            when {
                source.startsWith("http://") || source.startsWith("https://") ->
                    java.net.URL(source).openStream()
                source.startsWith("file://") ->
                    java.io.File(source.removePrefix("file://")).inputStream()
                source.startsWith("audio/") || source.startsWith("/") || runCatching { java.io.File(source).exists() }.getOrDefault(false) ->
                    if (source.startsWith("audio/")) {
                        DesktopPlayer::class.java.classLoader.getResourceAsStream(source)
                            ?: java.io.File(source).inputStream()
                    } else {
                        java.io.File(source).inputStream()
                    }
                else -> java.io.File(source).inputStream()
            }
        } catch (e: Exception) { onComplete(); return false }

        if (stream == null) { onComplete(); return false }

        stopped = false
        paused = false
        Thread {
            try {
                val ais = AudioSystem.getAudioInputStream(stream)
                val decoded = AudioSystem.getAudioInputStream(ais.format, ais)
                val info = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, decoded.format)
                val line = AudioSystem.getLine(info) as SourceDataLine
                current = line
                line.open(decoded.format)
                line.start()
                val buf = ByteArray(8192)
                var n = decoded.read(buf)
                while (!stopped && n != -1) {
                    if (paused) {
                        // 暂停：停掉声卡但保留流位置，恢复后接着播，不会丢进度。
                        runCatching { line.stop() }
                        while (paused && !stopped) Thread.sleep(50)
                        if (stopped) break
                        runCatching { line.start() }
                        continue
                    }
                    line.write(buf, 0, n)
                    n = decoded.read(buf)
                }
                line.drain()
                line.stop()
                line.close()
                decoded.close()
                stream.close()
                if (!stopped) onComplete()
            } catch (e: Exception) {
                onComplete()
            } finally {
                current = null
            }
        }.start()
        return true
    }

    fun pause() {
        paused = true
        runCatching { current?.stop() }
    }

    fun resume() {
        paused = false
        runCatching { current?.start() }
    }

    fun isPaused(): Boolean = paused

    fun stop() {
        stopped = true
        paused = false
        runCatching { current?.close() }
        current = null
    }
}
