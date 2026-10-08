package com.example.rulearn.ui.words

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.rulearn.ui.theme.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLDecoder

/** 网易有道词典的常见包名，装了就直接跳它。 */
private val YOUDAO_PACKAGES = listOf(
    "com.youdao.dict",      // 网易有道词典
    "com.youdao.hindict",  // 有道词典 HD
    "com.netease.youdao"   // 网易有道
)

/** 掌握度按钮的连点冷却时间（毫秒）：冷却期内忽略新点击。 */
private const val CLICK_COOLDOWN_MS = 700L

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

    val mastery = remember(word, progress) { ProgressStore.of(progress, word) }
    val entry = local ?: WordEntry(word, online?.translation ?: "")

    // 掌握度：限制连点，否则狂点会让 seen/ok/bad 计数虚高、复习间隔失真。
    // cooling 只在「被判定为连点（冷却期内重复点击）」时为真、用于提示；
    // 普通单次点击正常记录、不弹提示。冷却结束后由 LaunchedEffect 自动复位。
    var lastClick by remember(word) { mutableLongStateOf(0L) }
    var cooling by remember(word) { mutableStateOf(false) }
    LaunchedEffect(cooling) {
        if (cooling) {
            delay(CLICK_COOLDOWN_MS)
            cooling = false
        }
    }
    fun answer(ok: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastClick < CLICK_COOLDOWN_MS) {
            // 冷却期内重复点击：判定为连点，弹提示并忽略本次记录，避免计数虚高
            cooling = true
            Toast.makeText(ctx, "操作过于频繁，请稍候再试", Toast.LENGTH_SHORT).show()
            return
        }
        lastClick = now
        AppRepository.recordAnswer(word, ok)
        Toast.makeText(ctx, if (ok) "已标记：记住了 ✓" else "已标记：忘记了", Toast.LENGTH_SHORT).show()
    }

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
            // 单词主体（原先上方那块 WordArt 装饰图按要求移除了）
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            word,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!online?.phonetic.isNullOrBlank()) {
                            Text(
                                "音标：${online?.phonetic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(
                            "Lv.${mastery.level} · 学 ${mastery.seen} 次 · 对 ${mastery.ok} / 错 ${mastery.bad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    IconButton(onClick = { Speaker.speak(word, false) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "系统朗读", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { Speaker.speak(word, true) }) {
                        Icon(Icons.Filled.Language, contentDescription = "网络发音", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("释义", fontWeight = FontWeight.Bold)
                    val text = when {
                        local != null -> local.zh
                        loading -> "正在联网查询…"
                        online != null -> online!!.translation
                        else -> "（暂无释义）"
                    }
                    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
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
                            "系统未装俄语语音引擎，当前使用网络发音。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // 掌握度操作（带连点冷却）
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("掌握度", fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { answer(false) },
                            enabled = !cooling,
                            modifier = Modifier.weight(1f)
                        ) { Text("忘记了") }
                        Button(
                            onClick = { answer(true) },
                            enabled = !cooling,
                            modifier = Modifier.weight(1f)
                        ) { Text("记住了") }
                    }
                    if (cooling) {
                        Text(
                            "操作过于频繁，请稍候再试",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { openYoudao(ctx, word) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("有道词典", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = { openTtsInstall(ctx) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("语音包", Modifier.padding(start = 6.dp))
                }
            }
            Text(
                "「有道词典」：已安装 App 会直接打开查词；没装则跳有道网页版。" +
                    "「语音包」：俄语离线语音需系统 TTS 引擎，App 不附带语音文件（通常几十 MB），" +
                    "未装引擎时会打开引擎下载页。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "提示：Wiktionary 在部分网络环境下可能无法访问，打不开属正常现象。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://ru.wiktionary.org/wiki/${Uri.encode(word)}"))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("在 Wiktionary 查看（需联网，可能不可用）", Modifier.padding(start = 6.dp))
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

/** 装了网易有道词典就直接打开查词，没装就跳有道网页版。 */
private fun openYoudao(ctx: android.content.Context, word: String) {
    val pm = ctx.packageManager
    val installed = YOUDAO_PACKAGES.firstOrNull { pkg ->
        runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
    }
    if (installed != null) {
        val intent = Intent("com.youdao.dict.action.SEARCH").apply {
            setPackage(installed)
            putExtra("query", word)
        }
        if (runCatching { ctx.startActivity(intent) }.isSuccess) return
    }
    val web = "https://dict.youdao.com/result?word=${Uri.encode(word)}&lang=ru"
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web))) }
}

/** 打开系统 TTS 引擎安装页；没有可用引擎时改为打开引擎下载页。 */
private fun openTtsInstall(ctx: android.content.Context) {
    if (Speaker.hasAnyTtsEngine(ctx)) {
        runCatching { ctx.startActivity(Intent("android.speech.tts.action.INSTALL_TTS_DATA")) }
    } else {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")))
        }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")
                    )
                )
            }
        }
    }
}
