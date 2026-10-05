package com.example.rulearn.nav

/**
 * 跨平台导航状态。不依赖 AndroidX Navigation，桌面与 Android 共用同一套路由。
 * 用法：在 Compose 中保存为 remember { Router() }，或用单例。
 */
object Route {
    const val HOME = "home"
    const val WORDS = "words"
    const val ALPHABET = "alphabet"
    const val ME = "me"
    const val LISTEN = "listen"
    const val SPELLING = "spelling"
    const val QUIZ = "quiz"
    const val BOOKS = "books"
}

class Router {
    private val _stack = mutableListOf(Route.HOME)
    val stack: List<String> get() = _stack

    val current: String get() = _stack.last()

    fun navigate(route: String) {
        if (current == route) return
        // 底部 Tab 之间切换不应堆叠
        if (route in listOf(Route.HOME, Route.WORDS, Route.ALPHABET, Route.ME)) {
            _stack.removeAll { it in listOf(Route.HOME, Route.WORDS, Route.ALPHABET, Route.ME) }
            if (_stack.isEmpty()) _stack.add(Route.HOME)
        }
        if (!_stack.contains(route)) _stack.add(route)
    }

    fun back() {
        if (_stack.size > 1) _stack.removeAt(_stack.lastIndex)
    }
}
