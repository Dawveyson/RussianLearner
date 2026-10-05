package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.data.AppRepository

/** 用原生文件对话框选文件，返回绝对路径或 null。 */
private fun pickFile(): String? {
    val fd = java.awt.FileDialog(java.awt.Frame(), "选择文件", java.awt.FileDialog.LOAD)
    fd.isVisible = true
    val f = fd.file ?: return null
    return (fd.directory ?: "") + f
}

@Composable
fun BooksScreen(navigate: (String) -> Unit) {
    val books by AppRepository.vocabBooks.collectAsState()
    val lessons by AppRepository.lessons.collectAsState()
    var pasteText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    ScreenScroll {
        SectionTitle("导入词库（JSON / TSV）")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val p = pickFile()
                if (p != null) {
                    val t = java.io.File(p).readText(Charsets.UTF_8)
                    msg = AppRepository.importBookText(t)?.let { "已导入：$it" } ?: "导入失败：内容为空或格式不对"
                }
            }) { Text("选择文件") }
            OutlinedButton(onClick = {
                val u = urlText.trim()
                if (u.isNotEmpty()) msg = AppRepository.downloadBook(u)?.let { "已导入：$it" } ?: "下载/导入失败"
            }) { Text("从 URL 导入") }
        }
        OutlinedTextField(
            value = urlText, onValueChange = { urlText = it },
            label = { Text("词库 URL（可选）") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Spacer(8)
        OutlinedTextField(
            value = pasteText, onValueChange = { pasteText = it },
            label = { Text("或粘贴词库文本") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            maxLines = 6
        )
        TextButton(onClick = {
            if (pasteText.isNotBlank()) msg = AppRepository.importBookText(pasteText)?.let { "已导入：$it" } ?: "导入失败"
        }) { Text("粘贴导入") }

        SectionTitle("导入课文（JSON）")
        OutlinedButton(onClick = {
            val p = pickFile()
            if (p != null) {
                val t = java.io.File(p).readText(Charsets.UTF_8)
                val n = AppRepository.importLessonsText(t)
                msg = if (n > 0) "已导入 $n 课" else "导入失败：格式不对"
            }
        }) { Text("选择课文 JSON 文件") }

        SectionTitle("导入 ZIP 音频书")
        OutlinedButton(onClick = {
            val p = pickFile()
            if (p != null) {
                val n = AppRepository.importLessonZip(p)
                msg = if (n > 0) "已导入 $n 课（音频已解压）" else "导入失败：包不合规范"
            }
        }) { Text("选择 ZIP 文件") }

        if (msg.isNotEmpty()) {
            Spacer(8)
            Text(msg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(16)
        SectionTitle("我的词库（${books.size}）")
        LazyColumn(Modifier.fillMaxWidth()) {
            items(books) { b ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(b.name, style = MaterialTheme.typography.titleMedium)
                            Text("${b.entries.size} 词", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { AppRepository.deleteBook(b.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }

        SectionTitle("我的课文（${lessons.size}）")
        LazyColumn(Modifier.fillMaxWidth()) {
            items(lessons) { l ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(l.title, style = MaterialTheme.typography.titleMedium)
                            Text("${l.segments.size} 句", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { AppRepository.deleteLesson(l.n) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
