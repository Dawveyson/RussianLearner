package com.example.rulearn.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * 轻量间隔重复（SRS）记录。按「俄语词条原文小写」为键，跨词书共享掌握度。
 * level 0..6，复习间隔遵循艾宾浩斯遗忘曲线的大致梯度：
 * 当天(0) → 1天 → 2天 → 4天 → 7天 → 15天 → 30天。
 * 答对升级（间隔拉长），答错降级（回到更短的间隔重新巩固）。
 */
object ProgressStore {

    private val INTERVAL_DAYS = longArrayOf(0, 1, 2, 4, 7, 15, 30)
    private const val MAX_LEVEL = 6
    private const val FILE = "progress.json"

    fun load(ctx: Context): Map<String, Mastery> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyMap()
        return runCatching {
            val o = JSONObject(f.readText())
            val out = mutableMapOf<String, Mastery>()
            o.keys().forEach { k ->
                val v = o.getJSONObject(k)
                out[k] = Mastery(
                    level = v.optInt("lvl", 0).coerceIn(0, MAX_LEVEL),
                    due = v.optLong("due", 0L),
                    seen = v.optInt("seen", 0),
                    ok = v.optInt("ok", 0),
                    bad = v.optInt("bad", 0)
                )
            }
            out
        }.getOrDefault(emptyMap())
    }

    fun save(ctx: Context, map: Map<String, Mastery>) {
        val o = JSONObject()
        map.forEach { (k, v) ->
            o.put(k, JSONObject().put("lvl", v.level).put("due", v.due)
                .put("seen", v.seen).put("ok", v.ok).put("bad", v.bad))
        }
        runCatching { File(ctx.filesDir, FILE).writeText(o.toString()) }
    }

    fun of(map: Map<String, Mastery>, ru: String): Mastery =
        map[ru.trim().lowercase()] ?: Mastery()

    /** 答对升级、答错降级，返回新的记录。 */
    fun answer(map: Map<String, Mastery>, ru: String, correct: Boolean): Mastery {
        val key = ru.trim().lowercase()
        val cur = map[key] ?: Mastery()
        val level = if (correct) (cur.level + 1).coerceAtMost(MAX_LEVEL) else (cur.level - 1).coerceAtLeast(0)
        return Mastery(
            level = level,
            due = System.currentTimeMillis() + INTERVAL_DAYS[level] * 86_400_000L,
            seen = cur.seen + 1,
            ok = cur.ok + if (correct) 1 else 0,
            bad = cur.bad + if (!correct) 1 else 0
        )
    }

    fun clear(ctx: Context) {
        runCatching { File(ctx.filesDir, FILE).delete() }
    }
}
