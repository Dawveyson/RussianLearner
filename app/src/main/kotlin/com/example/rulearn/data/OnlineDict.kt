package com.example.rulearn.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 联网查词结果。 */
data class OnlineMeaning(
    val translation: String,
    val phonetic: String? = null
)

/**
 * 国内可访问的俄中在线查词。
 * 未配置密钥时使用有道演示接口，配置了 appKey/appSecret 时走签名 openapi。
 */
object OnlineDict {

    suspend fun lookup(word: String, appKey: String?, appSecret: String?): OnlineMeaning? =
        if (!appKey.isNullOrBlank() && !appSecret.isNullOrBlank()) {
            lookupSigned(word, appKey, appSecret) ?: lookupDemo(word)
        } else {
            lookupDemo(word)
        }

    suspend fun lookupDemo(word: String): OnlineMeaning? = withContext(Dispatchers.IO) {
        val json = fetchUrl("https://aidemo.youdao.com/trans?q=${urlEncode(word)}&from=ru&to=zh-CHS")
            ?: return@withContext null
        runCatching {
            val o = JSONObject(json)
            if (o.optString("errorCode") != "0") return@withContext null
            val tr = o.optJSONArray("translation")?.optString(0)?.takeIf { it.isNotBlank() }
                ?: return@withContext null
            OnlineMeaning(tr)
        }.getOrNull()
    }

    suspend fun lookupSigned(word: String, appKey: String, appSecret: String): OnlineMeaning? =
        withContext(Dispatchers.IO) {
            val salt = System.nanoTime().toString()
            val curtime = (System.currentTimeMillis() / 1000).toString()
            val input = if (word.length <= 20) word else word.take(10) + word.length + word.takeLast(10)
            val sign = sha256(appKey + input + salt + curtime + appSecret)
            val params = listOf(
                "q" to word, "from" to "ru", "to" to "zh-CHS", "appKey" to appKey,
                "salt" to salt, "sign" to sign, "signType" to "v3", "curtime" to curtime
            )
            val url = "https://openapi.youdao.com/api?" +
                params.joinToString("&") { "${it.first}=${urlEncode(it.second)}" }
            val json = fetchUrl(url) ?: return@withContext null
            runCatching {
                val o = JSONObject(json)
                if (o.optString("errorCode") != "0") return@withContext null
                val tr = o.optJSONArray("translation")?.optString(0)?.takeIf { it.isNotBlank() }
                    ?: return@withContext null
                val phonetic = o.optJSONObject("basic")?.optString("phonetic")?.takeIf { it.isNotBlank() }
                OnlineMeaning(tr, phonetic)
            }.getOrNull()
        }

    /**
     * 网络发音（有道 dictvoice），免密钥。
     * 注意：俄语必须用 le=ru，用 type=2 服务端会返回 "returned null audio"（拿不到音频）。
     */
    fun audioUrl(word: String): String =
        "https://dict.youdao.com/dictvoice?audio=${urlEncode(word)}&le=ru"
}

/** 简单 GET，10 秒超时，失败返回 null。 */
fun fetchUrl(url: String): String? = runCatching {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("User-Agent", "RuLearn/1.0")
        setRequestProperty("Referer", "https://www.youdao.com/")
    }
    try {
        if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
        conn.inputStream.bufferedReader().readText()
    } finally {
        conn.disconnect()
    }
}.getOrNull()

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")
