package com.example.rulearn.ui.me

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.BuildConfig
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.BackupStore
import com.example.rulearn.data.RemoteConfigCache
import com.example.rulearn.ui.Route
import com.example.rulearn.ui.theme.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by AppRepository.stats.collectAsStateWithLifecycle()

    var goalText by remember { mutableStateOf(stats.dailyGoal.toString()) }
    var goalMsg by remember { mutableStateOf("") }

    var key by remember { mutableStateOf(Prefs.apiKey(ctx)) }
    var secret by remember { mutableStateOf(Prefs.apiSecret(ctx)) }
    var keyMsg by remember { mutableStateOf("") }

    var preferNet by remember { mutableStateOf(Prefs.preferNetworkTts(ctx)) }
    var confirmReset by remember { mutableStateOf(false) }

    // ---- 备份与更新 ----
    var backupMsg by remember { mutableStateOf("") }
    var showUpdate by remember { mutableStateOf(false) }
    var showAnnounce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { RemoteConfigCache.refresh() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val json = BackupStore.exportJson(ctx)
        runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
        }.onSuccess {
            backupMsg = "已导出学习进度（含掌握度与设置）。把文件放到桌面后，运行 tools/backup_progress.sh 可备份并上传 GitHub。"
            Toast.makeText(ctx, "已导出", Toast.LENGTH_SHORT).show()
        }.onFailure {
            backupMsg = "导出失败：${it.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) {
            backupMsg = "导入失败：无法读取文件"
            return@rememberLauncherForActivityResult
        }
        val ok = BackupStore.importJson(ctx, text)
        backupMsg = if (ok) "已导入学习进度，掌握度与设置已恢复。" else "导入失败：文件格式不对或已损坏。"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("每日目标", fontWeight = FontWeight.Bold)
                    Text(
                        "首页进度环按这个数字计算今日学习进度（5–200）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        goalText,
                        { goalText = it.filter(Char::isDigit) },
                        label = { Text("每天学多少个词") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = {
                        val v = goalText.toIntOrNull() ?: 20
                        Prefs.setDailyGoal(ctx, v)
                        AppRepository.refreshStats()
                        goalMsg = "已保存：每天 $v 个"
                    }) { Text("保存") }
                    if (goalMsg.isNotBlank()) {
                        Text(goalMsg, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("在线词典（有道智云 · 国内可访问）", fontWeight = FontWeight.Bold)
                    Text(
                        "不填也能用：内置演示接口。填入免费申请的 appKey / appSecret 后更稳定、额度更高。申请：ai.youdao.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(key, { key = it }, label = { Text("appKey") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(secret, { secret = it }, label = { Text("appSecret") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        Prefs.setApiKey(ctx, key)
                        Prefs.setApiSecret(ctx, secret)
                        keyMsg = "已保存"
                    }) { Text("保存密钥") }
                    if (keyMsg.isNotBlank()) Text(keyMsg, color = MaterialTheme.colorScheme.primary)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("发音", fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("优先使用网络发音（有道）", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferNet,
                            onCheckedChange = { preferNet = it; Prefs.setPreferNetworkTts(ctx, it) }
                        )
                    }
                    Text(
                        "系统未装俄语语音包时会自动改用网络发音。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching { ctx.startActivity(Intent("android.speech.tts.action.INSTALL_TTS_DATA")) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("下载 / 安装俄语语音包")
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("学习数据", fontWeight = FontWeight.Bold)
                    Text(
                        "已掌握 ${stats.mastered} 个词 · 连续 ${stats.streak} 天 · 今日已学 ${stats.learnedToday}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("清除全部掌握度记录") }
                }
            }

            // ---- 备份与更新 ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("备份与更新", fontWeight = FontWeight.Bold)
                    Text(
                        "导出会把「掌握度 + 全部设置/统计」打包成一个 JSON；导入可跨设备恢复。导出后把文件放到桌面，运行 tools/backup_progress.sh 即可同步到桌面并上传 GitHub。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val name = "rulearn-progress-" +
                                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date()) + ".json"
                            exportLauncher.launch(name)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导出学习进度") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导入学习进度") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val cfg = withContext(Dispatchers.IO) { RemoteConfigCache.refresh() }
                                    val hasNew = (cfg?.latestVersionCode ?: 0) > BuildConfig.VERSION_CODE
                                    showUpdate = true
                                    if (!hasNew) {
                                        backupMsg = "已是最新版本（v${BuildConfig.VERSION_NAME}）。"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("检查更新") }
                        OutlinedButton(
                            onClick = { showAnnounce = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("查看公告") }
                    }
                    if (backupMsg.isNotBlank()) {
                        Text(backupMsg, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("关于", fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { nav.navigate(Route.LICENSES) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("开源许可 / Open Source Licenses") }
                    Text(
                        "RuLearner v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    "Made by Davey With Heart💗 · VibeCoding",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("清除掌握度？") },
            text = { Text("所有单词的等级与复习时间会被清空，词库本身不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.resetProgress()
                    confirmReset = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            }
        )
    }

    // 更新对话框
    if (showUpdate) {
        val cfg = RemoteConfigCache.config.value
        val hasNew = (cfg?.latestVersionCode ?: 0) > BuildConfig.VERSION_CODE
        AlertDialog(
            onDismissRequest = { showUpdate = false },
            title = { Text(if (hasNew) "发现新版本" else "已是最新") },
            text = {
                if (hasNew) {
                    Text("最新版本：v${cfg?.latestVersionName ?: ""}（versionCode ${cfg?.latestVersionCode}）\n当前版本：v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                } else {
                    Text("当前已是最新版本 v${BuildConfig.VERSION_NAME}。")
                }
            },
            confirmButton = {
                if (hasNew && !cfg?.apkUrl.isNullOrBlank()) {
                    TextButton(onClick = {
                        showUpdate = false
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, cfg!!.apkUrl.toUri())) }
                    }) { Text("去下载") }
                } else {
                    TextButton(onClick = { showUpdate = false }) { Text("好的") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdate = false }) { Text("关闭") }
            }
        )
    }

    // 公告对话框
    if (showAnnounce) {
        val anns = RemoteConfigCache.config.value?.announcements ?: emptyList()
        AlertDialog(
            onDismissRequest = { showAnnounce = false },
            title = { Text("公告") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (anns.isEmpty()) {
                        Text("暂无可显示公告。")
                    } else {
                        anns.forEach { a ->
                            Column {
                                Text(a.title.ifBlank { "公告" }, fontWeight = FontWeight.Bold)
                                if (a.date.isNotBlank()) {
                                    Text(a.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(a.body)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnnounce = false }) { Text("关闭") }
            }
        )
    }
}
