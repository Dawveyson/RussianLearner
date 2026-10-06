package com.example.rulearn.data

import android.content.Context
import com.example.rulearn.BuildConfig
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地学习进度（应用数据）的导出 / 导入。
 * 导出内容 = SRS 掌握度（progress.json）+ 全部 SharedPreferences 偏好与统计。
 * 格式为单个 JSON 文件，便于备份、跨设备恢复，也便于脚本同步到桌面 / GitHub。
 */
object BackupStore {

    fun exportJson(ctx: Context): String {
        val progress = ProgressStore.load(ctx)
        val root = JSONObject()
        root.put("app", "RuLearner")
        root.put("schema", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("versionCode", BuildConfig.VERSION_CODE)
        root.put("versionName", BuildConfig.VERSION_NAME)

        val p = JSONObject()
        progress.forEach { (k, v) ->
            p.put(
                k,
                JSONObject()
                    .put("lvl", v.level).put("due", v.due)
                    .put("seen", v.seen).put("ok", v.ok).put("bad", v.bad)
            )
        }
        root.put("progress", p)

        val prefs = JSONObject()
        val sp = ctx.getSharedPreferences("rulearn_prefs", Context.MODE_PRIVATE)
        sp.all.forEach { (k, v) -> prefs.put(k, v.toString()) }
        root.put("prefs", prefs)

        return root.toString(2)
    }

    fun importJson(ctx: Context, text: String): Boolean {
        return runCatching {
            val root = JSONObject(text)
            val p = root.optJSONObject("progress") ?: JSONObject()
            val map = mutableMapOf<String, Mastery>()
            p.keys().forEach { k ->
                val v = p.getJSONObject(k)
                map[k] = Mastery(
                    level = v.optInt("lvl", 0).coerceIn(0, 5),
                    due = v.optLong("due", 0L),
                    seen = v.optInt("seen", 0),
                    ok = v.optInt("ok", 0),
                    bad = v.optInt("bad", 0)
                )
            }
            ProgressStore.save(ctx, map)

            root.optJSONObject("prefs")?.let { prefs ->
                val sp = ctx.getSharedPreferences("rulearn_prefs", Context.MODE_PRIVATE)
                val ed = sp.edit()
                prefs.keys().forEach { k -> ed.putString(k, prefs.optString(k)) }
                ed.apply()
            }

            AppRepository.refreshStats()
            true
        }.getOrDefault(false)
    }
}
