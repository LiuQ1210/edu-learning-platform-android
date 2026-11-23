package com.github.learningplatform.data.remote.dto

/**
 * 内容观看积分结算结果（课程视频 / 帖子共用）。
 *
 * 服务端 `/learn/report` 与 `/points/watch` 返回的积分字段一致，客户端只需一套模型。
 */
data class ContentWatchResult(
    /** 本次实际入账积分：0 或 5 */
    val awardedPoints: Int = 0,
    /** 当天课程 + 帖子合计已得积分（服务端权威值） */
    val todayContentPoints: Int = 0,
    /** 当天内容积分上限，固定 50 */
    val todayContentLimit: Int = 0,
    /** 该内容当天累计观看秒数 */
    val watchedSeconds: Int = 0
)

/** 本周某一天的学习时长（后端 weekMinutes 数组项） */
data class WeekMinuteDto(
    val date: String = "",
    val weekday: String = "",
    /** 当天总分钟（课程 + 文章） */
    val minutes: Int = 0,
    /** 当天课程分钟；后端未返回该字段时为 null */
    val courseMinutes: Int? = null,
    /** 当天文章阅读分钟；后端未返回该字段时为 null */
    val articleMinutes: Int? = null
)

/**
 * 学习数据总览（后端 GET /api/v1/learn/stats）。
 *
 * 只统计课程播放与文章阅读，短视频不计入。
 * 分来源字段（todayCourseMinutes / courseMinutes / articleMinutes 等）为可空：
 * 后端未返回时页面降级为只显示合计，返回后自动展示构成。
 */
data class StudyStatsDto(
    val todayMinutes: Int = 0,
    val continuousDays: Int = 0,
    /** 今日课程分钟（分来源，可空） */
    val todayCourseMinutes: Int? = null,
    /** 今日文章阅读分钟（分来源，可空） */
    val todayArticleMinutes: Int? = null,
    val weekMinutes: List<WeekMinuteDto> = emptyList(),
    /** 已完成课程数 */
    val completedCourses: Int = 0,
    /** 在学课程数（有进度未完成，可空） */
    val learningCourses: Int? = null,
    /** 累计课程学习分钟（可空） */
    val totalCourseMinutes: Int? = null,
    /** 今日阅读文章篇数（可空） */
    val readPostsToday: Int? = null,
    /** 本周阅读文章篇数（可空） */
    val readPostsWeek: Int? = null,
    /** 本周文章阅读分钟（可空） */
    val articleMinutesWeek: Int? = null,
    /** 笔记条数 */
    val noteCount: Int = 0
)

/**
 * 我的课程（后端 GET /api/v1/learn/courses?status=learning|completed）。
 *
 * 只返回有观看记录的课程：learning = 看过小节但没全部学完；
 * completed = 该课程未删除的小节全部 finished。
 */
data class LearningCourseDto(
    val courseId: Long = 0,
    val courseTitle: String = "",
    val coverUrl: String = "",
    val totalLessons: Int = 0,
    val finishedLessons: Int = 0,
    /** 已完成小节占比，0~100 */
    val progressPercent: Int = 0,
    /** 最近学习的小节标题 */
    val lastLessonTitle: String = "",
    val lastStudyTime: String = ""
)
