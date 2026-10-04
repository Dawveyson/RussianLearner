package com.example.rulearn.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rulearn.ui.alphabet.AlphabetScreen
import com.example.rulearn.ui.alphabet.HandwritingScreen
import com.example.rulearn.ui.common.AppBottomBar
import com.example.rulearn.ui.common.NavItem
import com.example.rulearn.ui.home.HomeScreen
import com.example.rulearn.ui.lesson.LessonScreen
import com.example.rulearn.ui.me.ImportScreen
import com.example.rulearn.ui.me.MeScreen
import com.example.rulearn.ui.me.SettingsScreen
import com.example.rulearn.ui.words.QuizScreen
import com.example.rulearn.ui.words.SpellingScreen
import com.example.rulearn.ui.words.WordDetailScreen
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

    val TOP_LEVEL = setOf(HOME, WORDS, ALPHABET, ME)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in Route.TOP_LEVEL

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
        }
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
