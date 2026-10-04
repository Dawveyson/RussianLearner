package com.example.rulearn.ui.words

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.WordBook
import com.example.rulearn.ui.Route
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.common.RuKeyboard
import com.example.rulearn.ui.common.SectionTitle
import com.example.rulearn.ui.theme.GlassCard
import java.net.URLEncoder

@Composable
fun WordsScreen(nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("词库", "查词", "练习")

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
        }
        when (tab) {
            0 -> BookTab(nav)
            1 -> SearchTab(nav)
            else -> PracticeTab(nav)
        }
    }
}

@Composable
private fun BookTab(nav: NavController) {
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val progress by AppRepository.progress.collectAsStateWithLifecycle()
    val books: List<WordBook> = remember(vocab, lessons) { AppRepository.practiceBooks() }

    if (books.isEmpty()) {
        EmptyHint("还没有词库。去「个人 → 导入资源」添加吧", Modifier.fillMaxSize())
        return
    }

    var selectedId by remember(books) { mutableStateOf(books.first().id) }
    val book = books.firstOrNull { it.id == selectedId } ?: books.first()

    Column(Modifier.fillMaxSize()) {
        // 词书横向选择
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(books, key = { it.id }) { b ->
                val active = b.id == selectedId
                Surface(
                    onClick = { selectedId = b.id },
                    shape = RoundedCornerShape(50),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${b.name} · ${b.entries.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        var query by remember(book.id) { mutableStateOf("") }
        val entries = remember(book, query) {
            if (query.isBlank()) book.entries
            else book.entries.filter {
                it.ru.contains(query.trim(), true) || it.zh.contains(query.trim(), true)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("在本词书中筛选") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true
        )

        if (entries.isEmpty()) {
            EmptyHint("没有匹配的词条")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries.take(400), key = { it.ru }) { e ->
                    GlassCard(
                        onClick = {
                            nav.navigate("${Route.WORD.substringBefore("/{")}/${URLEncoder.encode(e.ru, "UTF-8")}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.ru, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    e.zh.ifBlank { "（无释义）" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            val lv = progress[e.ru.trim().lowercase()]?.level ?: 0
                            if (lv > 0) {
                                Text(
                                    "Lv.$lv",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTab(nav: NavController) {
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showKb by remember { mutableStateOf(true) }
    // 依赖词书，导入/删除词库后搜索结果才会重算
    val results = remember(query, vocab, lessons) { AppRepository.search(query) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("输入俄语单词 / 中文") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showKb = !showKb }) {
                    Text(if (showKb) "⌨" else "🔠", fontSize = 18.sp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            singleLine = true
        )

        if (results.isEmpty()) {
            EmptyHint(if (query.isBlank()) "输入俄语即可查词，例如 «спасибо»" else "没找到 «$query»")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.ru }) { e ->
                    GlassCard(
                        onClick = {
                            nav.navigate("${Route.WORD.substringBefore("/{")}/${URLEncoder.encode(e.ru, "UTF-8")}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.ru, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    e.zh.ifBlank { "（无释义）" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showKb) {
            RuKeyboard(
                onChar = { query = (query + it).lowercase() },
                onBackspace = { if (query.isNotEmpty()) query = query.dropLast(1) },
                onSpace = { query = "$query " }
            )
        }
    }
}

@Composable
private fun PracticeTab(nav: NavController) {
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val books = remember(vocab, lessons) { AppRepository.practiceBooks() }
    val stats by AppRepository.stats.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("选择练习")
        PracticeEntry(
            icon = Icons.Filled.EditNote,
            title = "看中文写俄文",
            sub = "顺序 / 随机 · 即时判分",
            enabled = books.isNotEmpty()
        ) { nav.navigate(Route.SPELLING) }
        PracticeEntry(
            icon = Icons.Filled.AutoStories,
            title = "刷题闯关",
            sub = "选择题 · 拼写 · 连对闯关",
            enabled = books.isNotEmpty()
        ) { nav.navigate(Route.QUIZ) }

        if (books.isEmpty()) {
            EmptyHint("先导入词库或教材才能练习")
        } else {
            SectionTitle("词书概览")
            books.forEach { b ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(b.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${b.entries.size} 条 · ${if (b.hasAudio) "含音频" else "无音频"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (stats.quizBest > 0) {
            Text(
                "闯关历史最佳：${stats.quizBest} / 10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PracticeEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    GlassCard(onClick = if (enabled) onClick else null, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, enabled = enabled) { Text("开始") }
        }
    }
}
