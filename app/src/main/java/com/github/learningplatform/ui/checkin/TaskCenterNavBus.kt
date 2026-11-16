package com.github.learningplatform.ui.checkin

import kotlinx.coroutines.flow.MutableStateFlow

/** 任务中心「去完成」要跳到的主框架分区 */
enum class TaskCenterTarget {
    /** 课程视频入口：回主框架的「首页」课程 Tab，不再跳短视频 Tab */
    COURSE,
    COMMUNITY
}

/**
 * 任务中心 → 主框架的跳转桥。
 *
 * 任务中心是根 NavHost 的全屏页，主框架的底部 Tab 在内层 NavHost 里，
 * 全屏页拿不到内层 NavController。这里用一个单例状态把「要跳哪个分区」
 * 交给 MainScaffold，由它在内层导航里切换，避免把导航引用泄漏到全屏页。
 */
object TaskCenterNavBus {

    val pending = MutableStateFlow<TaskCenterTarget?>(null)

    fun request(target: TaskCenterTarget) {
        pending.value = target
    }

    fun consume() {
        pending.value = null
    }
}
