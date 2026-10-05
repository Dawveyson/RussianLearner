package com.example.rulearn.desktop.ui
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.rulearn.data.AppRepository
import com.example.rulearn.quiz.Question
import com.example.rulearn.quiz.QuizMode
import com.example.rulearn.quiz.buildQuestions
import com.example.rulearn.quiz.norm

@Composable
fun QuizScreen() {
    val bookList by AppRepository.vocabBooks.collectAsState()
    var mode by remember { mutableStateOf(QuizMode.ZH2RU) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var qi by remember { mutableStateOf(0) }
    var hearts by remember { mutableStateOf(3) }
    var score by remember { mutableStateOf(0) }
    var chosen by remember { mutableStateOf(-1) }
    var typing by remember { mutableStateOf("") }
    var fb by remember { mutableStateOf<String?>(null) }

    val finished = questions.isNotEmpty() && (qi >= questions.size || hearts <= 0)
    val q = if (!finished && qi < questions.size) questions[qi] else null

    fun handleAnswer(ok: Boolean, ru: String) {
        if (ok) score++ else hearts--
        AppRepository.recordAnswer(ru, ok)
        fb = if (ok) "正确！" else "正确：${if (q?.mode == QuizMode.SPELL) q.entry.ru else q?.options?.getOrNull(q.answerIndex).orEmpty()}"
    }

    ScreenScroll {
        if (bookList.isEmpty()) {
            Text("还没有词库。请先到「学习资源」导入词库。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@ScreenScroll
        }

        SectionTitle("题型")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == QuizMode.ZH2RU, onClick = { mode = QuizMode.ZH2RU }, label = { Text("中→俄") })
            FilterChip(selected = mode == QuizMode.RU2ZH, onClick = { mode = QuizMode.RU2ZH }, label = { Text("俄→中") })
            FilterChip(selected = mode == QuizMode.SPELL, onClick = { mode = QuizMode.SPELL }, label = { Text("拼写") })
        }

        Spacer(8)
        Button(onClick = {
            questions = buildQuestions(bookList, 10, 4)
            qi = 0; hearts = 3; score = 0; chosen = -1; typing = ""; fb = null
        }) { Text(if (questions.isEmpty()) "开始闯关" else "重新开始") }

        if (questions.isNotEmpty() && !finished) {
            Spacer(8)
            Text("生命 ❤ $hearts　得分 $score　第 ${qi + 1}/${questions.size}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(12)
        when {
            q != null -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(q.prompt, style = MaterialTheme.typography.headlineSmall)
                        Spacer(8)
                        if (q.mode == QuizMode.SPELL) {
                            OutlinedTextField(value = typing, onValueChange = { typing = it },
                                label = { Text("输入俄语") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                val ok = norm(typing) == norm(q.entry.ru)
                                handleAnswer(ok, q.entry.ru)
                                chosen = 0
                            }) { Text("提交") }
                        } else {
                            q.options.forEachIndexed { i, opt ->
                                Button(onClick = {
                                    chosen = i
                                    handleAnswer(i == q.answerIndex, q.entry.ru)
                                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(opt)
                                }
                            }
                        }
                        val fbNow = fb
                        if (fbNow != null) {
                            Text(fbNow, color = if (fbNow.startsWith("正确！")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        if (fb != null) {
                            Spacer(8)
                            Button(onClick = { qi++; chosen = -1; typing = ""; fb = null }, modifier = Modifier.fillMaxWidth()) { Text("下一题") }
                        }
                    }
                }
            }
            finished && hearts <= 0 -> {
                Text("闯关失败，最终得分 $score。", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            }
            finished -> {
                Text("闯关完成！最终得分 $score。", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
