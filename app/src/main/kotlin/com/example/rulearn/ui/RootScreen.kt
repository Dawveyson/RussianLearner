package com.example.rulearn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import androidx.core.net.toUri
import java.net.URLEncoder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.rulearn.BuildConfig
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.RemoteConfigCache
import com.example.rulearn.player.PocketLinesScreen
import com.example.rulearn.ui.alphabet.AlphabetScreen
import com.example.rulearn.ui.alphabet.HandwritingScreen
import com.example.rulearn.ui.common.AppBottomBar
import com.example.rulearn.ui.common.NavItem
import com.example.rulearn.ui.home.HomeScreen
import com.example.rulearn.ui.lesson.LessonScreen
import com.example.rulearn.ui.me.ImportScreen
import com.example.rulearn.ui.me.LicensesScreen
import com.example.rulearn.ui.me.MeScreen
import com.example.rulearn.ui.me.SettingsScreen
import com.example.rulearn.ui.words.QuizScreen
import com.example.rulearn.ui.words.SpellingScreen
import com.example.rulearn.ui.words.WordDetailScreen
import com.example.rulearn.ui.web.WebScreen
import com.example.rulearn.ui.words.WordsScreen

object Route {
    const val HOME = "home"
    const val WORDS = "words"
    const val ALPHABET = "alphabet"
    const val ME = "me"
    const val LESSON = "lesson/{n}"
    const val WORD = "word/{ru}"
    const val SPELLING = "spelling"
    const val QUIZ = "quiz"
    const val HANDWRITING = "handwriting"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val LICENSES = "licenses"
    const val POCKET_LINES = "pocket_lines"
    const val WEB = "web/{url}"

    /** 应用内浏览器路由：对整个 URL 编码，避免其中的 / ? & : 破坏路径分段。 */
    fun webUrl(u: String) = "web/${URLEncoder.encode(u, "UTF-8")}"

    val TOP_LEVEL = setOf(HOME, WORDS, ALPHABET, ME)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in Route.TOP_LEVEL

    var showStartupUpdate by remember { mutableStateOf(false) }
    var showAnnounce by remember { mutableStateOf(false) }

    /** 拉取服务器配置，返回未读公告数并更新角标。 */
    suspend fun pullAnnouncements(): Int {
        val cfg = RemoteConfigCache.refresh()
        val seen = Prefs.seenAnnouncementIds(ctx)
        val unseen = (cfg?.announcements ?: emptyList()).count { it.id.isNotBlank() && it.id !in seen }
        AnnounceUi.unseenCount.value = unseen
        Prefs.setLastAnnounceCheck(ctx, System.currentTimeMillis())
        return unseen
    }

    // 冷启动：拉取版本与公告；若有未读公告直接弹窗
    LaunchedEffect(Unit) {
        val cfg = RemoteConfigCache.refresh()
        if ((cfg?.latestVersionCode ?: 0) > BuildConfig.VERSION_CODE) showStartupUpdate = true
        val seen = Prefs.seenAnnouncementIds(ctx)
        val unseen = (cfg?.announcements ?: emptyList()).count { it.id.isNotBlank() && it.id !in seen }
        AnnounceUi.unseenCount.value = unseen
        Prefs.setLastAnnounceCheck(ctx, System.currentTimeMillis())
        if (unseen > 0) showAnnounce = true
    }

    // 首页火花旁按钮点击 → 重新拉取并弹窗（每次查看公告都从服务器拉一遍）
    LaunchedEffect(AnnounceUi.openRequest.value) {
        if (AnnounceUi.openRequest.value) {
            AnnounceUi.openRequest.value = false
            pullAnnouncements()
            showAnnounce = true
        }
    }

    // 进程存活期间，间隔 6 小时自动拉取，有新公告直接弹窗
    LaunchedEffect(Unit) {
        while (true) {
            delay(6 * 60 * 60 * 1000L)
            if (pullAnnouncements() > 0) showAnnounce = true
        }
    }

    val items = listOf(
        NavItem("首页", Icons.Outlined.Home),
        NavItem("单词", Icons.AutoMirrored.Outlined.MenuBook),
        NavItem("字母", Icons.Outlined.TextFields),
        NavItem("个人", Icons.Outlined.Person)
    )
    val selected = when (currentRoute) {
        Route.WORDS -> 1
        Route.ALPHABET -> 2
        Route.ME -> 3
        else -> 0
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                AppBottomBar(
                    items = items,
                    selectedIndex = selected,
                    onSelect = { index -> nav.navigateToTab(index) }
                )
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Route.HOME,
            modifier = Modifier.padding(pad)
        ) {
            composable(Route.HOME) { HomeScreen(nav) }
            composable(Route.WORDS) { WordsScreen(nav) }
            composable(Route.ALPHABET) { AlphabetScreen(nav) }
            composable(Route.ME) { MeScreen(nav) }

            composable(
                Route.LESSON,
                arguments = listOf(navArgument("n") { type = NavType.IntType })
            ) { back ->
                LessonScreen(nav, back.arguments?.getInt("n") ?: 1)
            }
            composable(
                Route.WORD,
                arguments = listOf(navArgument("ru") { type = NavType.StringType })
            ) { back ->
                WordDetailScreen(nav, back.arguments?.getString("ru") ?: "")
            }
            composable(Route.SPELLING) { SpellingScreen(nav) }
            composable(Route.QUIZ) { QuizScreen(nav) }
            composable(Route.HANDWRITING) { HandwritingScreen(nav) }
            composable(Route.IMPORT) { ImportScreen(nav) }
            composable(Route.SETTINGS) { SettingsScreen(nav) }
            composable(Route.LICENSES) { LicensesScreen(nav) }
            composable(Route.POCKET_LINES) { PocketLinesScreen(nav) }
            composable(
                Route.WEB,
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { back ->
                WebScreen(nav, back.arguments?.getString("url") ?: "")
            }
        }
    }

    if (showStartupUpdate) {
        val cfg = RemoteConfigCache.config.value
        AlertDialog(
            onDismissRequest = { showStartupUpdate = false },
            title = { Text("发现新版本") },
            text = {
                Text(
                    "最新版本：v${cfg?.latestVersionName ?: ""}（versionCode ${cfg?.latestVersionCode}）\n" +
                        "当前版本：v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）"
                )
            },
            confirmButton = {
                if (!cfg?.apkUrl.isNullOrBlank()) {
                    TextButton(onClick = {
                        showStartupUpdate = false
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, cfg!!.apkUrl.toUri())) }
                    }) { Text("去下载") }
                } else {
                    TextButton(onClick = { showStartupUpdate = false }) { Text("好的") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartupUpdate = false }) { Text("稍后") }
            }
        )
    }

    // 公告弹窗（冷启动/间隔自动拉取 + 首页按钮触发）
    if (showAnnounce) {
        val anns = RemoteConfigCache.config.value?.announcements ?: emptyList()
        val dismiss = {
            Prefs.markAnnouncementSeen(ctx, anns.map { it.id }.toSet())
            AnnounceUi.unseenCount.value = 0
            showAnnounce = false
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("公告") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                TextButton(onClick = dismiss) { Text("关闭") }
            }
        )
    }
}

private fun NavHostController.navigateToTab(index: Int) {
    val route = when (index) {
        1 -> Route.WORDS
        2 -> Route.ALPHABET
        3 -> Route.ME
        else -> Route.HOME
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
