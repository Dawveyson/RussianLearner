package com.example.rulearn.data

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 网络发音 / 取词。Android 与桌面共用（纯 java.net，无 Android 依赖）。
 */
object OnlineDict {

    /** 抓取 URL 文本（UTF-8），失败返回 null。 */
    fun fetchUrl(url: String): String? = runCatching {
        val conn = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "RuLearn/1.0")
        }
        conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }.getOrNull()

    /**
     * 有道词典发音地址（type=2 美式发音）。作为离线音频缺失时的回落。
     */
    fun audioUrl(word: String): String {
        val w = URLEncoder.encode(word.trim(), "UTF-8")
        return "https://dict.youdao.com/dictvoice?audio=$w&type=2"
    }
}
