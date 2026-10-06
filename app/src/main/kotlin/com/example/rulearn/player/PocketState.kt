package com.example.rulearn.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 随身听（后台服务）状态镜像，供 UI 观察。 */
object PocketState {
    var isPlaying by mutableStateOf(false)
    var currentIndex by mutableIntStateOf(-1)
    var total by mutableIntStateOf(0)
    var label by mutableStateOf("未在播放")

    /**
     * 歌词时间轴：当前播放列表每一段的展示文本（即“歌词”行）。
     * 顺序与随身听内部播放列表一致，下标即段号。
     */
    var lines by mutableStateOf<List<String>>(emptyList())

    /** 与 [lines] 一一对应的中文释义，供「音频同步词」页面双语显示。 */
    var linesZh by mutableStateOf<List<String>>(emptyList())

    /** 当前段内的播放进度（毫秒），用于进度条。 */
    var positionMs by mutableIntStateOf(0)
    /** 当前段总时长（毫秒）。 */
    var durationMs by mutableIntStateOf(0)

    val running: Boolean get() = currentIndex >= 0

    /** 跳转到指定段（歌词行）。 */
    fun seekIndex(ctx: android.content.Context, index: Int) {
        if (index in lines.indices) PocketService.send(ctx, PocketService.ACTION_SEEK_INDEX, index)
    }

    /** 在当前段内拖动到指定毫秒位置。 */
    fun seekMs(ctx: android.content.Context, ms: Int) {
        PocketService.send(ctx, PocketService.ACTION_SEEK_MS, ms = ms)
    }
}
