package com.example.rulearn.player

import android.content.Context

/**
 * 统一单词发音入口，集中处理三种音源：
 * 1) 词条自带音频（内嵌 assets / 本地文件 / URL）
 * 2) 离线语音包（开启「离线优先」且已缓存）
 * 3) 网络发音（有道 dictvoice，免密钥、国内可用）
 *
 * 这样既支持离线包，也天然规避「系统 TTS 在你国家/地区不可用」的问题。
 */
object Pronounce {

    fun play(
        ctx: Context,
        player: AudioPlayer,
        word: String,
        entryAudio: String = "",
        preferOffline: Boolean = false,
        autoDownload: Boolean = false
    ) {
        val w = word.trim().lowercase()
        // 自动下载：播放时若离线包缺失，后台静默补缓存（下一个词起就能离线读）
        if (autoDownload && entryAudio.isBlank() && !TtsPack.has(ctx, w)) {
            TtsPack.preload(ctx, listOf(w))
        }
        when {
            // 词条自带音频最高优先
            entryAudio.isNotBlank() -> player.play(entryAudio)
            // 本地离线包命中：直接播放缓存文件，秒出声、不依赖联网/系统 TTS（解决“发音慢”）
            TtsPack.has(ctx, w) -> player.play(TtsPack.path(ctx, w))
            // 否则才走网络发音兜底
            else -> Speaker.speak(word, preferNetwork = true)
        }
    }
}
