package com.example.rulearn.player

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.example.rulearn.data.OnlineDict
import com.example.rulearn.data.WordEntry
import java.util.Locale

/**
 * 单词朗读：优先/兜底切换系统 TTS（ru-RU）与有道网络发音。
 * TTS 引擎全局复用，避免每个页面重复创建。
 */
object Speaker {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var netPlayer: MediaPlayer? = null

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching { tts?.language = Locale("ru", "RU") }
                ready = true
            }
        }
    }

    val ttsReady: Boolean get() = ready

    /** 网络发音（有道 dictvoice），免密钥、国内可用。 */
    fun speakNetwork(word: String) {
        runCatching {
            netPlayer?.release()
            netPlayer = MediaPlayer().apply {
                setDataSource(OnlineDict.audioUrl(word))
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        }
    }

    /** 按偏好朗读：true 优先网络，false 优先系统 TTS。 */
    fun speak(word: String, preferNetwork: Boolean) {
        if (!preferNetwork && ready) {
            runCatching { tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null) }
        } else {
            speakNetwork(word)
        }
    }

    private var assetPlayer: MediaPlayer? = null

    /**
     * 播放内嵌到 App 的 assets 音频（离线，无需联网）。
     * asset 缺失或打开失败时回退到网络发音（fallbackText），保证总有声音。
     */
    fun playAsset(ctx: Context, assetPath: String, fallbackText: String) {
        runCatching {
            val afd = ctx.assets.openFd(assetPath)
            assetPlayer?.release()
            assetPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
            runCatching { afd.close() }
        }.onFailure {
            speakNetwork(fallbackText)
        }
    }

    fun release() {
        runCatching { netPlayer?.release() }
        netPlayer = null
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
