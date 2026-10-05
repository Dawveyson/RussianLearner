package com.example.rulearn.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.rulearn.data.AppRepository
import com.example.rulearn.desktop.player.DesktopPlayer
import com.example.rulearn.desktop.theme.RuLearnTheme
import com.example.rulearn.desktop.ui.AlphabetScreen
import com.example.rulearn.desktop.ui.BooksScreen
import com.example.rulearn.desktop.ui.HomeScreen
import com.example.rulearn.desktop.ui.ListenScreen
import com.example.rulearn.desktop.ui.MeScreen
import com.example.rulearn.desktop.ui.QuizScreen
import com.example.rulearn.desktop.ui.SpellingScreen
import com.example.rulearn.nav.Route
import com.example.rulearn.platform.KeyValueStore

fun main() = application {
    val dataDir = System.getProperty("user.home") + "/.rulearn"
    java.io.File(dataDir).mkdirs()
    val store = KeyValueStore("$dataDir/rulearn.properties")
    if (!AppRepository.ready) AppRepository.init(dataDir, store)

    Window(onCloseRequest = ::exitApplication, title = "RuLearn · 俄语学习") {
        RuLearnTheme {
            val player = remember { DesktopPlayer() }
            AppRoot(player = player)
        }
    }
}

private val TABS = listOf(
    Triple(Route.HOME, Icons.Filled.Home, "首页"),
    Triple(Route.WORDS, Icons.Filled.Book, "资源"),
    Triple(Route.ALPHABET, Icons.Filled.TextFormat, "字母"),
    Triple(Route.ME, Icons.Filled.Person, "我的")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(player: DesktopPlayer) {
    var route by remember { mutableStateOf(Route.HOME) }
    val navigate: (String) -> Unit = { r -> route = r }
    val back: () -> Unit = { route = Route.HOME }

    val tabRoute = route in listOf(Route.HOME, Route.WORDS, Route.ALPHABET, Route.ME)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(route)) },
                navigationIcon = {
                    if (!tabRoute) IconButton(onClick = back) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { (r, icon, label) ->
                    NavigationBarItem(
                        selected = route == r,
                        onClick = { route = r },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (route) {
                Route.HOME -> HomeScreen(navigate)
                Route.WORDS -> BooksScreen(navigate)
                Route.ALPHABET -> AlphabetScreen(player)
                Route.ME -> MeScreen()
                Route.LISTEN -> ListenScreen(player = player, navigate = navigate)
                Route.SPELLING -> SpellingScreen(player = player)
                Route.QUIZ -> QuizScreen()
            }
        }
    }
}

private fun titleFor(route: String): String = when (route) {
    Route.HOME -> "RuLearn · 俄语学习"
    Route.WORDS -> "学习资源"
    Route.ALPHABET -> "俄语字母"
    Route.ME -> "我的"
    Route.LISTEN -> "随身听"
    Route.SPELLING -> "拼写练习"
    Route.QUIZ -> "闯关测验"
    else -> "RuLearn"
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ScreenScroll(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp).then(modifier)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) { content() }
    }
}
