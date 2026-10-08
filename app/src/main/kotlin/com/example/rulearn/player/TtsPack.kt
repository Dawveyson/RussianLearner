package com.example.rulearn.player

import android.content.Context
import com.example.rulearn.data.OnlineDict
import com.example.rulearn.data.sha256
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 离线俄语发音包。
 *
 * 系统 TTS 引擎（如 Google 俄语语音）在国内部分应用商店/地区无法直接安装，
 * 会提示「此 app 在你的国家或地区不可用」。本模块把每个单词的网络发音
 * （有道 dictvoice，免密钥、国内可访问）下载并缓存到 App 私有目录，
 * 之后发音就完全不依赖系统 TTS 引擎、也不依赖联网，相当于一个可自动下载的
 * 「TTS 扩展包」。
 */
object TtsPack {

    private const val DIR = "tts_pack"

    /** 后台下载用的独立作用域，失败不拖垮调用方（也不会因未捕获异常闪退）。 */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
    )

    private fun dir(ctx: Context) = File(ctx.filesDir, DIR).also { if (!it.exists()) it.mkdirs() }

    /** 单词 → 缓存文件名（用哈希避免西里尔字符做文件名出问题）。 */
    private fun fileName(word: String): String = sha256(word.trim().lowercase()).take(16) + ".mp3"

    fun path(ctx: Context, word: String): String = File(dir(ctx), fileName(word)).absolutePath

    fun has(ctx: Context, word: String): Boolean = File(dir(ctx), fileName(word)).exists()

    /** 已缓存的单词数。 */
    fun count(ctx: Context): Int = runCatching { dir(ctx).listFiles()?.size ?: 0 }.getOrDefault(0)

    /** 清空离线包。 */
    fun clear(ctx: Context) = runCatching { dir(ctx).listFiles()?.forEach { it.delete() } }

    /**
     * 确保某个单词已缓存：已存在直接返回；否则下载。可在协程里调用。
     * @return true 表示本次确实下载了（用于统计）。
     */
    suspend fun ensure(ctx: Context, word: String): Boolean {
        val w = word.trim().lowercase()
        if (w.isBlank() || has(ctx, w)) return false
        val f = File(dir(ctx), fileName(w))
        return runCatching { downloadFile(OnlineDict.audioUrl(w), f); true }.getOrDefault(false)
    }

    /** 后台静默预下载（只下缺失的词），不阻塞调用方。 */
    fun preload(ctx: Context, words: List<String>) {
        scope.launch {
            words.forEach {
                runCatching { ensure(ctx, it) }
            }
        }
    }

    /**
     * 逐个下载指定单词的发音并写入缓存。
     * @param onProgress 每下载完一个回调 (已完成, 总数)。
     */
    suspend fun download(
        ctx: Context,
        words: List<String>,
        onProgress: (done: Int, total: Int) -> Unit
    ) {
        val list = words.map { it.trim().lowercase() }.distinct().filter { it.isNotBlank() }
        list.forEachIndexed { i, w ->
            val f = File(dir(ctx), fileName(w))
            if (!f.exists()) {
                runCatching { downloadFile(OnlineDict.audioUrl(w), f) }
            }
            onProgress(i + 1, list.size)
        }
    }

    /** 下载一个 URL 到本地文件（10s 超时）。 */
    private fun downloadFile(url: String, out: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "RuLearn/1.0")
            setRequestProperty("Referer", "https://www.youdao.com/")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
