package com.example.rulearn.platform

import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

/** 平台无关（JVM）的文件 / zip 工具，Android 与桌面共用。 */

fun readText(path: String): String? {
    val f = File(path)
    if (!f.exists()) return null
    return runCatching { f.readText(charset = Charsets.UTF_8) }.getOrNull()
}

fun writeText(path: String, text: String) {
    val f = File(path)
    f.parentFile?.mkdirs()
    runCatching { f.writeText(text, charset = Charsets.UTF_8) }
}

fun exists(path: String): Boolean = File(path).exists()

fun ensureDir(dir: String) {
    File(dir).mkdirs()
}

fun listFiles(dir: String): List<String> =
    File(dir).listFiles()?.map { it.absolutePath } ?: emptyList()

/**
 * 解压 zip 到 dest，带 zip-slip 防护。返回是否成功。
 */
fun unzipTo(zipPath: String, destDir: String): Boolean {
    val dest = File(destDir)
    val input = runCatching { File(zipPath).inputStream() }.getOrNull() ?: return false
    input.use { raw ->
        ZipInputStream(BufferedInputStream(raw)).use { zis ->
            dest.mkdirs()
            val destRoot = dest.canonicalFile
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(dest, entry.name)
                val inBounds = target.canonicalFile.path.startsWith(destRoot.path + File.separator) ||
                    target.canonicalFile == destRoot
                if (inBounds) {
                    if (entry.isDirectory) target.mkdirs()
                    else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zis.copyTo(it) }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
    return true
}
