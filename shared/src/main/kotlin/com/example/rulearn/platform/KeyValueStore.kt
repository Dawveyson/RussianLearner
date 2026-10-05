package com.example.rulearn.platform

import java.io.File
import java.util.Properties

/**
 * 简单的键值存储，基于 .properties 文件，Android 与桌面共用。
 * 相比 Android 的 SharedPreferences 更通用，避免平台分支。
 */
class KeyValueStore(private val file: String) {
    private val props = Properties()
    private var loaded = false

    private fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        val f = File(file)
        if (f.exists()) runCatching { f.inputStream().use { props.load(it) } }
    }

    private fun save() {
        val f = File(file)
        f.parentFile?.mkdirs()
        runCatching { f.outputStream().use { props.store(it, "rulearn") } }
    }

    fun getString(key: String, def: String): String {
        loadIfNeeded()
        return props.getProperty(key, def)
    }

    fun putString(key: String, value: String) {
        loadIfNeeded()
        props.setProperty(key, value)
        save()
    }

    fun getInt(key: String, def: Int): Int = getString(key, def.toString()).toIntOrNull() ?: def
    fun putInt(key: String, value: Int) = putString(key, value.toString())

    fun getBoolean(key: String, def: Boolean): Boolean = getString(key, def.toString()).toBoolean()
    fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())

    fun getLong(key: String, def: Long): Long = getString(key, def.toString()).toLongOrNull() ?: def
    fun putLong(key: String, value: Long) = putString(key, value.toString())
}
