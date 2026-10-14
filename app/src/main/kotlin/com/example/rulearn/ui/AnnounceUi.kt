package com.example.rulearn.ui

import androidx.compose.runtime.mutableStateOf

/**
 * 公告 UI 共享状态（顶层单例）。
 *
 * - 首页火花旁的圆形按钮点击后把 [openRequest] 置 true，由 RootScreen 收到后
 *   重新拉取服务器配置并弹出公告（满足“每次查看公告都从服务器拉一遍”）。
 * - [unseenCount] 驱动首页红点角标；[showDialog] 由 RootScreen 控制实际弹窗。
 * - 冷启动 / 长时间间隔后 RootScreen 自动拉取，若有未读公告直接弹窗。
 */
object AnnounceUi {
    val openRequest = mutableStateOf(false)
    val unseenCount = mutableStateOf(0)
    val showDialog = mutableStateOf(false)
}
