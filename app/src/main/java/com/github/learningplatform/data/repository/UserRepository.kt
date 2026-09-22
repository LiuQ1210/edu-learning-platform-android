package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
import com.github.learningplatform.data.remote.UserApi
import com.github.learningplatform.data.remote.dto.BlogHomeDto
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.UpdateProfileRequest
import com.github.learningplatform.data.remote.dto.UserProfileDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto
import javax.inject.Inject
import javax.inject.Singleton

/** 用户中心仓库（接口文档 v3.0 第三章，签到日历见 CheckinRepository） */
@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {

    suspend fun getProfile(): UserProfileDto {
        preview { return DemoData.profile }
        return safeApiCall { userApi.getProfile() }
    }

    suspend fun updateProfile(request: UpdateProfileRequest) {
        previewUnit { return }
        safeApiCallForUnit { userApi.updateProfile(request) }
    }

    /** 他人博客主页（公开） */
    suspend fun getBlogHome(userId: Long, pageNum: Int = 1, pageSize: Int = 20): BlogHomeDto {
        preview {
            return BlogHomeDto(
                userId = userId,
                nickname = "张三",
                avatar = "",
                bio = "热爱学习",
                articleCount = DemoData.articles.size,
                totalViewCount = 15600,
                totalLikeCount = 890,
                totalFavoriteCount = 230,
                articles = DemoData.articlePage(1, 20)
            )
        }
        return safeApiCall { userApi.getBlogHome(userId, pageNum, pageSize) }
    }

    suspend fun getMyCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        keyword: String? = null
    ): PageData<MyCourseDto> {
        preview { return DemoData.myCourses }
        return safeApiCall { userApi.getMyCourses(pageNum, pageSize, keyword) }
    }

    /** @return 是否加入成功 */
    suspend fun joinCourse(courseId: Long): Boolean {
        preview { return true }
        return safeApiCall { userApi.joinCourse(courseId) }.joined
    }

    suspend fun quitCourse(courseId: Long) {
        previewUnit { return }
        safeApiCallForUnit { userApi.quitCourse(courseId) }
    }

    /** @param targetType 1文章 2视频 */
    suspend fun getFavorites(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<UserTargetItemDto> {
        preview { return DemoData.favorites }
        return safeApiCall {
            userApi.getFavorites(pageNum, pageSize, targetType)
        }
    }

    suspend fun getLikes(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<UserTargetItemDto> {
        preview { return DemoData.favorites }
        return safeApiCall {
            userApi.getLikes(pageNum, pageSize, targetType)
        }
    }

    suspend fun getViewHistory(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<ViewHistoryItemDto> {
        preview { return DemoData.history }
        return safeApiCall {
            userApi.getViewHistory(pageNum, pageSize, targetType)
        }
    }

    /** 不传 targetType 则清空全部 */
    suspend fun clearViewHistory(targetType: Int? = null) {
        previewUnit { return }
        safeApiCallForUnit { userApi.clearViewHistory(targetType) }
    }

    suspend fun getDownloadRecords(
        pageNum: Int = 1,
        pageSize: Int = 20,
        resourceType: Int? = null
    ): PageData<DownloadRecordDto> {
        preview { return DemoData.downloads }
        return safeApiCall {
            userApi.getDownloadRecords(pageNum, pageSize, resourceType)
        }
    }
}
