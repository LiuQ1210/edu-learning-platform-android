package com.github.learningplatform.data.demo

/**
 * 预览模式短路器。
 *
 * `BuildConfig.UI_PREVIEW = true` 时，Repository 一进入就直接返回样例数据，
 * 不发任何网络请求 —— 界面能完整跑起来，后端没就绪也能评审。
 *
 * 用法（Repository 里每个方法开头一行）：
 * ```
 * fun getCourses(...): PageData<CourseDto> = preview { DemoData.coursePage(...) }
 *     ?: safeApiCall { courseApi.getCourses(...) }
 * ```
 * 关掉开关（`-PuiPreview=false`）后 [preview] 恒返回 null，走真实请求。
 *
 * 注意：这里不是「网络失败时兜底」，而是**编译期开关**。生产构建里
 * `DemoData.enabled` 是常量 false，R8 会把整条分支和样例数据一起裁掉。
 */
inline fun <T : Any> preview(block: () -> T): T? =
    if (DemoData.enabled) block() else null

/** 无返回值的写操作：预览模式下直接当成功处理。 */
inline fun previewUnit(block: () -> Unit): Boolean {
    if (DemoData.enabled) {
        block()
        return true
    }
    return false
}
