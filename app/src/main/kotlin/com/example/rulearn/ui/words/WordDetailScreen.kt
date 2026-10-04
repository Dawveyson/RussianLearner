package com.example.rulearn.ui.words

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.OnlineDict
import com.example.rulearn.data.OnlineMeaning
import com.example.rulearn.data.ProgressStore
import com.example.rulearn.data.WordEntry
import com.example.rulearn.player.Speaker
import com.example.rulearn.ui.common.LocalWordArtContentColor
import com.example.rulearn.ui.common.WordArt
import com.example.rulearn.ui.theme.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(nav: NavController, rawWord: String) {
    val ctx = LocalContext.current
    val word = remember(rawWord) { runCatching { URLDecoder.decode(rawWord, "UTF-8") }.getOrDefault(rawWord) }
    val local = remember(word) { AppRepository.lookup(word) }
    val progress by AppRepository.progress.collectAsStateWithLifecycle()

    var online by remember { mutableStateOf<OnlineMeaning?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(word) {
        loading = true
        online = withContext(Dispatchers.IO) {
            OnlineDict.lookup(word, Prefs.apiKey(ctx), Prefs.apiSecret(ctx))
        }
        loading = false
    }

    val preferNet = Prefs.preferNetworkTts(ctx)
    // 从已收集的 progress 取值，才能在「记住了/忘记了」后立即刷新 Lv 与对错次数；
    // 若用 AppRepository.masteryOf() 读 StateFlow.value，Compose 不会跟踪，界面不更新。
    val mastery = remember(word, progress) { ProgressStore.of(progress, word) }
    val entry = local ?: WordEntry(word, online?.translation ?: "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(word, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 单词配图卡
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        WordArt(word, Modifier.matchParentSize()) {
                            Text(
                                word.take(2),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalWordArtContentColor.current.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                word,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!online?.phonetic.isNullOrBlank()) {
                                Text(
                                    "音标：${online?.phonetic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { Speaker.speak(word, false) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "系统朗读", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { Speaker.speak(word, true) }) {
                            Icon(Icons.Filled.Language, contentDescription = "网络发音", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 释义
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("释义", fontWeight = FontWeight.Bold)
                    val text = when {
                        local != null -> local.zh
                        loading -> "正在联网查询…"
                        online != null -> online!!.translation
                        else -> "（暂无释义）"
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (local == null && online != null) {
                        Text(
                            "来自在线词典（离线词库中没有该词）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (!Speaker.ttsReady) {
                        Text(
                            "系统未装俄语语音引擎，已自动使用网络发音。可在「设置」安装离线语音包。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // 掌握度
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("掌握度 Lv.${mastery.level}", fontWeight = FontWeight.Bold)
                    Text(
                        "学习 ${mastery.seen} 次 · 答对 ${mastery.ok} · 答错 ${mastery.bad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { AppRepository.recordAnswer(word, false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("忘记了") }
                        Button(
                            onClick = { AppRepository.recordAnswer(word, true) },
                            modifier = Modifier.weight(1f)
                        ) { Text("记住了") }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://ru.wiktionary.org/wiki/${Uri.encode(word)}"))
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Wiktionary", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = {
                        runCatching { ctx.startActivity(Intent("android.speech.tts.action.INSTALL_TTS_DATA")) }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("安装语音包", Modifier.padding(start = 6.dp))
                }
            }

            if (entry.note.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
