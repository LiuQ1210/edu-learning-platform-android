package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingResultDto
import javax.inject.Inject
import javax.inject.Singleton

/** 课程视频仓库（接口文档 v3.0 第六章） */
@Singleton
class CourseRepository @Inject constructor(
    private val courseApi: CourseApi
) {

    /** @param videoType 1短视频 2长视频；@param sort 1最新 2最热 3评分 */
    suspend fun getCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        videoType: Int? = null,
        sort: Int? = null
    ): PageData<CourseDto> {
        preview { return DemoData.coursePage(pageNum, pageSize, videoType) }
        return safeApiCall {
            courseApi.getCourses(pageNum, pageSize, categoryId, videoType, sort)
        }
    }

    suspend fun getCourseDetail(courseId: Long): CourseDetailDto {
        preview { return DemoData.courseDetail(courseId) }
        return safeApiCall { courseApi.getCourseDetail(courseId) }
    }

    /** 播放凭证：校验权限后返回带签名的限时播放地址 */
    suspend fun getPlayInfo(courseId: Long, lessonId: Long? = null): PlayInfoDto {
        preview { return DemoData.playInfo(courseId, lessonId) }
        return safeApiCall { courseApi.getPlayInfo(courseId, lessonId) }
    }

    /** 播放进度心跳（建议 15-30 秒一次） */
    suspend fun syncProgress(request: ProgressSyncRequest) {
        previewUnit { return }
        safeApiCallForUnit { courseApi.syncProgress(request) }
    }

    suspend fun rateCourse(courseId: Long, score: Double): RatingResultDto {
        preview { return DemoData.rateResult(score) }
        return safeApiCall { courseApi.rateCourse(courseId, com.github.learningplatform.data.remote.dto.RatingRequest(score)) }
    }

    suspend fun getRatings(courseId: Long, pageNum: Int = 1, pageSize: Int = 20): PageData<RatingDto> {
        preview { return DemoData.ratings(courseId) }
        return safeApiCall { courseApi.getRatings(courseId, pageNum, pageSize) }
    }
}
