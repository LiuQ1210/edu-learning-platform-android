package com.github.learningplatform.data.remote

/**
 * 后端 `targetType` 是**字符串枚举**，不是数字。这是踩过的坑：
 * 早期客户端传 `"1"` / `"2"`，后端直接返回
 * 「内容类型不合法（只支持 post / course / shortVideo）」。
 *
 * 后端各模块取值并不完全一致：
 *  - 互动（点赞/收藏/评论）：`post` / `course` / `shortVideo`
 *  - 观看历史：`post` / `course` / `shortVideo`
 *  - 笔记：`post` / `video`
 *
 * 客户端内部仍沿用数字（1=文章 2=视频课程 3=短视频），
 * 请求前用 [of] / [ofNote] 转成字符串，解析响应时用 [toInt] 转回数字。
 */
object ApiTargetType {

    const val POST = "post"
    const val COURSE = "course"
    const val SHORT_VIDEO = "shortVideo"

    /** 笔记模块专用（后端只认 post / video） */
    const val NOTE_VIDEO = "video"

    /** 客户端 1/2/3 -> 互动、历史接口的字符串 */
    fun of(type: Int?): String? = when (type) {
        1 -> POST
        2 -> COURSE
        3 -> SHORT_VIDEO
        else -> null
    }

    /**
     * 互动切换/状态/计数、评论接口用的是**数字** `1/2/3`
     * （后端 `Interaction.isValidTargetType` / `Comment.isValidTargetType` 只认数字）。
     *
     * 这一点和后端其它接口（myFavorites / history / note 用字符串）不一致，
     * 传错就会收到「参数不合法」或「内容类型不合法」。
     */
    fun ofNumeric(type: Int?): String? = when (type) {
        1, 2, 3 -> type.toString()
        else -> null
    }

    /** 客户端 1/2 -> 笔记接口的字符串（视频统一映射成 video） */
    fun ofNote(type: Int?): String? = when (type) {
        1 -> POST
        2 -> NOTE_VIDEO
        3 -> NOTE_VIDEO
        else -> null
    }

    /**
     * 后端字符串 -> 客户端数字：1 文章 / 2 视频课程 / 3 短视频。
     *
     * 后端行里返回的是 `post` / `course` / `shortVideo`（历史、收藏、点赞），
     * 笔记行返回 `post` / `video`。兼容数字字符串（老数据）与空值。
     */
    fun toInt(raw: String?): Int = when (raw?.trim()?.lowercase()) {
        POST -> 1
        COURSE -> 2
        SHORT_VIDEO -> 3
        NOTE_VIDEO -> 2
        else -> raw?.trim()?.toIntOrNull() ?: 1
    }

    /**
     * 清空观看历史时后端**必须指定类型**（不支持清空全部），
     * 所以「全部」需要按三类依次清。
     */
    val ALL: List<String> = listOf(POST, COURSE, SHORT_VIDEO)
}
