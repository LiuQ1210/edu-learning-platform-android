package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.ApiTargetType
import com.github.learningplatform.data.remote.UserApi
import com.github.learningplatform.data.remote.dto.BlogHomeDto
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.UpdateProfileRequest
import com.github.learningplatform.data.remote.dto.UserHomeDto
import com.github.learningplatform.data.remote.dto.UserProfileDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户中心仓库。
 *
 * 已与后端对齐：个人资料（读/写）、我的收藏、我的点赞、观看历史（列表/清空）。
 * 后端尚未提供、继续用本地样例渲染：我的课程 / 加入退出课程 / 下载记录 / 他人博客主页。
 */
@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {

    suspend fun getProfile(): UserProfileDto {
        val d = userApi.getProfile().okBody().payload()
        return UserProfileDto(
            userId = d.long("userId"),
            username = d.str("userName"),
            nickname = d.str("userName"),
            avatar = d.str("avatar"),
            bio = d.str("bio"),
            // 后端 sex：0男 1女 2不公开；客户端 gender：1男 2女 0未知
            gender = when (d.int("sex", 2)) {
                0 -> 1
                1 -> 2
                else -> 0
            }
        )
    }

    /**
     * 我的主页聚合数据：资料 + 关注数 / 粉丝数 / 获赞数。
     *
     * 后端 `GET /api/v1/user/home` 平铺返回这些字段，只针对**当前登录用户**。
     */
    suspend fun getUserHome(): UserHomeDto {
        val d = userApi.getUserHome().okBody().payload()
        return UserHomeDto(
            userId = d.long("userId"),
            userName = d.str("userName"),
            avatar = d.str("avatar"),
            sex = d.int("sex", 2),
            sexLabel = d.str("sexLabel"),
            bio = d.str("bio"),
            followCount = d.int("followCount"),
            fansCount = d.int("fansCount"),
            likeReceivedCount = d.int("likeReceivedCount")
        )
    }

    suspend fun updateProfile(request: UpdateProfileRequest) {
        // 客户端 gender：1男 2女 0未知 -> 后端 sex：0男 1女 2不公开
        val sex = when (request.gender) {
            1 -> "0"
            2 -> "1"
            0 -> "2"
            else -> null
        }
        userApi.updateProfile(
            userName = request.nickname,
            avatar = request.avatar,
            sex = sex,
            bio = request.bio
        ).okBody()
    }

    /** 他人博客主页：后端未提供，使用本地样例 */
    suspend fun getBlogHome(userId: Long, pageNum: Int = 1, pageSize: Int = 20): BlogHomeDto =
        BlogHomeDto(
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

    /** 我的课程：后端未提供，使用本地样例 */
    suspend fun getMyCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        keyword: String? = null
    ): PageData<MyCourseDto> = DemoData.myCourses

    /** 加入课程：后端未提供，本地直接当成功 */
    suspend fun joinCourse(courseId: Long): Boolean = true

    /** 退出课程：后端未提供 */
    suspend fun quitCourse(courseId: Long) = Unit

    /** @param targetType 1文章 2视频 */
    suspend fun getFavorites(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<UserTargetItemDto> {
        val rows = userApi.getFavorites(ApiTargetType.of(targetType)).okTable().rows
        val list = rows.map {
            UserTargetItemDto(
                targetType = ApiTargetType.toInt(it.str("targetType")),
                targetId = it.long("targetId"),
                title = it.str("title"),
                cover = it.str("coverUrl"),
                collectTime = it.str("createTime")
            )
        }
        return PageData(total = list.size.toLong(), list = list)
    }

    suspend fun getLikes(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<UserTargetItemDto> {
        val rows = userApi.getLikes(ApiTargetType.of(targetType)).okTable().rows
        val list = rows.map {
            UserTargetItemDto(
                targetType = ApiTargetType.toInt(it.str("targetType")),
                targetId = it.long("targetId"),
                title = it.str("title"),
                cover = it.str("coverUrl"),
                collectTime = it.str("createTime")
            )
        }
        return PageData(total = list.size.toLong(), list = list)
    }

    suspend fun getViewHistory(
        pageNum: Int = 1,
        pageSize: Int = 20,
        targetType: Int? = null
    ): PageData<ViewHistoryItemDto> {
        val rows = userApi.getViewHistory(ApiTargetType.of(targetType)).okTable().rows
        val list = rows.map {
            ViewHistoryItemDto(
                targetType = ApiTargetType.toInt(it.str("targetType")),
                targetId = it.long("targetId"),
                title = it.str("title"),
                cover = it.str("coverUrl"),
                progressSeconds = it.int("position"),
                lastViewTime = it.str("lastViewTime")
            )
        }
        return PageData(total = list.size.toLong(), list = list)
    }

    /**
     * 清空观看历史。
     *
     * 后端 `/api/v1/history/clear` **必须指定 targetType**，不提供「清空全部」，
     * 所以 targetType 为 null（UI 的「全部」）时按 post / course / shortVideo 依次清。
     */
    suspend fun clearViewHistory(targetType: Int? = null) {
        val types = ApiTargetType.of(targetType)?.let { listOf(it) } ?: ApiTargetType.ALL
        types.forEach { userApi.clearViewHistory(it).okBody() }
    }

    /** 下载记录：后端未提供，使用本地样例 */
    suspend fun getDownloadRecords(
        pageNum: Int = 1,
        pageSize: Int = 20,
        resourceType: Int? = null
    ): PageData<DownloadRecordDto> {
        val filtered = if (resourceType == null) DemoData.downloads
        else DemoData.downloads.copy(
            list = DemoData.downloads.list.filter { it.resourceType == resourceType }
        )
        return filtered
    }
}
