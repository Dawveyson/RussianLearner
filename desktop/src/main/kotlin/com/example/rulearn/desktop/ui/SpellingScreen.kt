package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.RU_KEYBOARD_ROWS
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.OnlineDict
import com.example.rulearn.desktop.player.DesktopPlayer
import com.example.rulearn.quiz.norm

@Composable
fun SpellingScreen(player: DesktopPlayer) {
    val bookList by AppRepository.vocabBooks.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    val book = bookList.firstOrNull { it.id == (selectedId ?: bookList.firstOrNull()?.id) } ?: bookList.firstOrNull()

    var idx by remember { mutableStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var okCount by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }

    ScreenScroll {
        if (bookList.isEmpty()) {
            Text("还没有词库。请先到「学习资源」导入词库。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@ScreenScroll
        }

        SectionTitle("选择词书")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bookList.take(8).forEach { b ->
                FilterChip(
                    selected = book?.id == b.id,
                    onClick = { selectedId = b.id; idx = 0; typed = ""; feedback = null },
                    label = { Text(b.name) }
                )
            }
        }

        Spacer(12)
        val entry = book?.entries?.getOrNull(idx % (book.entries.size))
        if (entry != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("请拼写：", style = MaterialTheme.typography.bodyMedium)
                    Text(entry.zh, style = MaterialTheme.typography.headlineSmall)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            val src = entry.audio.ifBlank { OnlineDict.audioUrl(entry.ru) }
                            player.play(src)
                        }) { Icon(Icons.Filled.PlayArrow, contentDescription = "发音") }
                        Text("进度：${idx + 1} / ${book.entries.size}　正确 $okCount / 共 $total", Modifier.padding(top = 8.dp))
                    }
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it },
                        label = { Text("输入俄语") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val correct = norm(typed) == norm(entry.ru)
                            AppRepository.recordAnswer(entry.ru, correct)
                            total++; if (correct) okCount++
                            feedback = if (correct) "正确！" else "正确写法：${entry.ru}"
                        }) { Text("提交") }
                        Button(onClick = {
                            idx = (idx + 1) % book.entries.size
                            typed = ""; feedback = null
                        }) { Text("下一个") }
                    }
                    val fbNow = feedback
                    if (fbNow != null) {
                        Text(fbNow, color = if (fbNow.startsWith("正确！")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(12)
            SectionTitle("俄文键盘")
            RU_KEYBOARD_ROWS.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { ch ->
                        Button(onClick = { typed += ch }, modifier = Modifier.weight(1f)) { Text(ch) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { typed = typed.dropLast(1) }, modifier = Modifier.weight(1f)) { Text("退格") }
                Button(onClick = { typed = "" }, modifier = Modifier.weight(1f)) { Text("清空") }
            }
        }
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
