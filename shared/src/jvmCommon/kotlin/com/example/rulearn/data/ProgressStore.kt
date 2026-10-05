package com.example.rulearn.data

import com.example.rulearn.platform.readText
import com.example.rulearn.platform.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 轻量间隔重复（SRS）：按「俄语原文小写」为键，跨词书共享掌握度。
 * level 0..5，对应复习间隔 0/1/2/4/7/15 天。
 */
object ProgressStore {
    private val INTERVAL_DAYS = longArrayOf(0, 1, 2, 4, 7, 15)
    private const val FILE = "progress.json"

    fun load(dir: String): Map<String, Mastery> {
        val text = readText("$dir/$FILE") ?: return emptyMap()
        return runCatching {
            val o = Json.parseToJsonElement(text).jsonObject
            val out = mutableMapOf<String, Mastery>()
            o.keys.forEach { k ->
                val v = o[k]!!.jsonObject
                out[k] = Mastery(
                    level = v["lvl"]?.let { it.toString().toIntOrNull() ?: 0 }?.coerceIn(0, 5) ?: 0,
                    due = v["due"]?.let { it.toString().toLongOrNull() ?: 0L } ?: 0L,
                    seen = v["seen"]?.let { it.toString().toIntOrNull() ?: 0 } ?: 0,
                    ok = v["ok"]?.let { it.toString().toIntOrNull() ?: 0 } ?: 0,
                    bad = v["bad"]?.let { it.toString().toIntOrNull() ?: 0 } ?: 0
                )
            }
            out
        }.getOrDefault(emptyMap())
    }

    fun save(dir: String, map: Map<String, Mastery>) {
        val obj = buildJsonObject {
            map.forEach { (k, v) ->
                put(k, buildJsonObject {
                    put("lvl", v.level); put("due", v.due)
                    put("seen", v.seen); put("ok", v.ok); put("bad", v.bad)
                })
            }
        }
        writeText("$dir/$FILE", obj.toString())
    }

    fun of(map: Map<String, Mastery>, ru: String): Mastery =
        map[ru.trim().lowercase()] ?: Mastery()

    /** 答对升级、答错降级，返回新的记录。 */
    fun answer(map: Map<String, Mastery>, ru: String, correct: Boolean): Mastery {
        val key = ru.trim().lowercase()
        val cur = map[key] ?: Mastery()
        val level = if (correct) (cur.level + 1).coerceAtMost(5) else (cur.level - 1).coerceAtLeast(0)
        return Mastery(
            level = level,
            due = System.currentTimeMillis() + INTERVAL_DAYS[level] * 86_400_000L,
            seen = cur.seen + 1,
            ok = cur.ok + if (correct) 1 else 0,
            bad = cur.bad + if (!correct) 1 else 0
        )
    }

    fun clear(dir: String) {
        val f = java.io.File("$dir/$FILE")
        runCatching { f.delete() }
    }
}
