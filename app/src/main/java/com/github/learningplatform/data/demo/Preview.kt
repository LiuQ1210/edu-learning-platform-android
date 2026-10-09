package com.github.learningplatform.data.demo

import com.github.learningplatform.BuildConfig

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

/**
 * 【任务⑦ 混合模式】内容域独立预览短路器。
 *
 * 与 [preview] 的区别：它跟随 `BuildConfig.CONTENT_PREVIEW`（默认 true），
 * 与 `UI_PREVIEW` 解耦——认证域（登录/注册）走真实后端时，
 * 内容域（分类树/课程/文章/个人中心）仍可返回本地死数据，避免「登录成功但主页全红」。
 *
 * 用法与 [preview] 一致（Repository 方法开头一行）：
 * ```
 * fun getCourses(...): PageData<CourseDto> = contentPreview { DemoData.coursePage(...) }
 *     ?: safeApiCall { courseApi.getCourses(...) }
 * ```
 * 后端内容域接口就绪后，用 `-PcontentPreview=false` 构建即切回真实接口。
 */
inline fun <T : Any> contentPreview(block: () -> T): T? =
    if (BuildConfig.CONTENT_PREVIEW) block() else null

/** 内容域写操作：CONTENT_PREVIEW=true 时直接当成功处理。 */
inline fun contentPreviewUnit(block: () -> Unit): Boolean {
    if (BuildConfig.CONTENT_PREVIEW) {
        block()
        return true
    }
    return false
}
