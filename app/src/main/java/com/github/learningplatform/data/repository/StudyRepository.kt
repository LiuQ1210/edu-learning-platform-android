package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.listOf
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.StudyApi
import com.github.learningplatform.data.remote.dto.LearningCourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.remote.dto.WeekMinuteDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** 学习数据仓库：学习时长上报之外，提供学习数据总览。 */
@Singleton
class StudyRepository @Inject constructor(
    private val studyApi: StudyApi
) {

    /**
     * 学习数据总览。
     *
     * 只统计课程播放与文章阅读；分来源字段后端未返回时保持 null，页面自动降级。
     */
    suspend fun getStats(): StudyStatsDto {
        val d = studyApi.getStudyStats().okBody().payload()
        return StudyStatsDto(
            todayMinutes = d.int("todayMinutes"),
            continuousDays = d.int("continuousDays"),
            todayCourseMinutes = d.intOrNull("todayCourseMinutes"),
            todayArticleMinutes = d.intOrNull("todayArticleMinutes"),
            weekMinutes = d.listOf("weekMinutes").map { (it as? JsonObject).toWeekMinute() },
            completedCourses = d.int("completedCourses"),
            learningCourses = d.intOrNull("learningCourses"),
            totalCourseMinutes = d.intOrNull("totalCourseMinutes"),
            readPostsToday = d.intOrNull("readPostsToday"),
            readPostsWeek = d.intOrNull("readPostsWeek"),
            articleMinutesWeek = d.intOrNull("articleMinutesWeek"),
            noteCount = d.int("noteCount")
        )
    }

    /**
     * 我的课程：status 取 learning / completed（后端必填，无默认值）。
     *
     * 只返回有观看记录的课程；学习数据页取一页 20 条即可。
     */
    suspend fun getLearningCourses(
        status: String,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<LearningCourseDto> {
        val table = studyApi.getLearningCourses(status, pageNum, pageSize).okTable()
        return PageData(
            total = table.total,
            list = table.rows.map { row ->
                LearningCourseDto(
                    courseId = row.long("courseId"),
                    courseTitle = row.str("courseTitle"),
                    coverUrl = row.str("coverUrl"),
                    totalLessons = row.int("totalLessons"),
                    finishedLessons = row.int("finishedLessons"),
                    progressPercent = row.int("progressPercent"),
                    lastLessonTitle = row.str("lastLessonTitle"),
                    lastStudyTime = row.str("lastStudyTime")
                )
            }
        )
    }

    private fun JsonObject?.toWeekMinute(): WeekMinuteDto {
        if (this == null) return WeekMinuteDto()
        return WeekMinuteDto(
            date = str("date"),
            weekday = str("weekday"),
            minutes = int("minutes"),
            courseMinutes = intOrNull("courseMinutes"),
            articleMinutes = intOrNull("articleMinutes")
        )
    }

    /** 字段缺失返回 null（区别于后端明确返回 0） */
    private fun JsonObject.intOrNull(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull
}
