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
        // 自动下载：播放时若离线包缺失，后台静默补缓存（下一个词起就能离线读）
        if (autoDownload && entryAudio.isBlank() && !TtsPack.has(ctx, word)) {
            TtsPack.preload(ctx, listOf(word))
        }
        when {
            entryAudio.isNotBlank() -> player.play(entryAudio)
            preferOffline && TtsPack.has(ctx, word) -> player.play(TtsPack.path(ctx, word))
            else -> Speaker.speak(word, preferNetwork = true)
        }
    }
}
