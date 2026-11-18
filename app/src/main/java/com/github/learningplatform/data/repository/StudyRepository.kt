package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.listOf
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.StudyApi
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.remote.dto.WeekMinuteDto
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** 学习数据仓库：学习时长上报之外，提供学习数据总览。 */
@Singleton
class StudyRepository @Inject constructor(
    private val studyApi: StudyApi
) {

    /** 学习数据总览（今日 / 连续天数 / 本周时长 / 已完成课程 / 笔记 / 待办） */
    suspend fun getStats(): StudyStatsDto {
        val d = studyApi.getStudyStats().okBody().payload()
        return StudyStatsDto(
            todayMinutes = d.int("todayMinutes"),
            continuousDays = d.int("continuousDays"),
            weekMinutes = d.listOf("weekMinutes").map { (it as? JsonObject).toWeekMinute() },
            completedCourses = d.int("completedCourses"),
            noteCount = d.int("noteCount"),
            todoCount = d.int("todoCount")
        )
    }

    private fun JsonObject?.toWeekMinute(): WeekMinuteDto {
        if (this == null) return WeekMinuteDto()
        return WeekMinuteDto(
            date = str("date"),
            weekday = str("weekday"),
            minutes = int("minutes")
        )
    }
}
