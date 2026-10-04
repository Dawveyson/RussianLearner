package com.example.rulearn.player

/**
 * 播放互斥总线：同一时间只允许一个声音在响。
 *
 * 背景：随身听（PocketService，前台服务里的 MediaPlayer）和页面里的
 * [AudioPlayer] 是两套完全独立的播放器，互不感知，所以会出现
 * 「教材在放、随身听也在放」的双响。这里做一层协调，谁开始发声，
 * 就把另一个（以及其它页面的播放器）暂停掉。
 *
 * 同进程单例，直接持有实例；不走广播是为了暂停能立即生效。
 */
object PlaybackBus {

    private val locals = LinkedHashSet<AudioPlayer>()
    private var pocket: PocketService? = null

    @Synchronized
    fun register(p: AudioPlayer) {
        locals.add(p)
    }

    @Synchronized
    fun unregister(p: AudioPlayer) {
        locals.remove(p)
    }

    @Synchronized
    fun attachPocket(s: PocketService) {
        pocket = s
    }

    @Synchronized
    fun detachPocket(s: PocketService) {
        if (pocket === s) pocket = null
    }

    /** 页面播放器开始发声：暂停随身听和其它页面的播放器。 */
    @Synchronized
    fun onLocalPlay(owner: AudioPlayer) {
        locals.forEach { if (it !== owner) runCatching { it.pause() } }
        runCatching { pocket?.pause() }
    }

    /** 随身听开始/恢复发声：暂停所有页面播放器。 */
    @Synchronized
    fun onPocketPlay() {
        locals.forEach { runCatching { it.pause() } }
    }
}
