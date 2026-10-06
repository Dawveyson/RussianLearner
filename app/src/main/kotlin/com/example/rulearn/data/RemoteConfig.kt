package com.example.rulearn.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 远程配置：版本信息与公告，托管在 GitHub（docs/config.json）。
 * 通过 jsDelivr CDN 分发，国内可访问。
 * 用户（开发者）只需编辑仓库里的 docs/config.json 并提交即可更新公告 / 版本号。
 */
const val REMOTE_CONFIG_URL =
    "https://cdn.jsdelivr.net/gh/Dawveyson/RussianLearner@main/docs/config.json"

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val date: String,
    val level: String
)

data class RemoteConfig(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val announcements: List<Announcement>
)

object RemoteConfigCache {
    /** 最近一次拉取到的配置（可被 Compose 观察）。 */
    val config = mutableStateOf<RemoteConfig?>(null)
    val lastError = mutableStateOf<String?>(null)

    suspend fun refresh(): RemoteConfig? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(REMOTE_CONFIG_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode != 200) {
                lastError.value = "HTTP ${conn.responseCode}"
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(text)
            val arr = o.optJSONArray("announcements")
            val anns = mutableListOf<Announcement>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    anns += Announcement(
                        id = a.optString("id", ""),
                        title = a.optString("title", ""),
                        body = a.optString("body", ""),
                        date = a.optString("date", ""),
                        level = a.optString("level", "info")
                    )
                }
            }
            val cfg = RemoteConfig(
                latestVersionCode = o.optInt("latestVersionCode", 0),
                latestVersionName = o.optString("latestVersionName", ""),
                apkUrl = o.optString("apkUrl", ""),
                announcements = anns
            )
            config.value = cfg
            lastError.value = null
            cfg
        } catch (e: Exception) {
            lastError.value = e.message
            Log.w("RemoteConfig", "fetch failed: ${e.message}")
            null
        }
    }
}
