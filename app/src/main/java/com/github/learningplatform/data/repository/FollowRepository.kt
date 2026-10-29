package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.longList
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.FollowApi
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/** 关注仓库（对接后端 ApiV1 接口，字段平铺在 AjaxResult 顶层） */
@Singleton
class FollowRepository @Inject constructor(
    private val followApi: FollowApi
) {

    /** 关注某人 */
    suspend fun addFollow(targetUserId: Long): Map<String, Any> {
        followApi.addFollow(targetUserId).okBody()
        return emptyMap()
    }

    /** 取消关注 */
    suspend fun cancelFollow(targetUserId: Long): Map<String, Any> {
        followApi.cancelFollow(targetUserId).okBody()
        return emptyMap()
    }

    /** 批量查询已关注的用户ID */
    suspend fun getFollowStatus(userIds: List<Long>): List<Long> {
        val body = followApi.getFollowStatus(userIds.joinToString(",")).okBody()
        // 后端：ajax.put("ids", ...)
        return body.longList("ids").ifEmpty { body.payload().longList("ids") }
    }

    /** 查粉丝数 + 关注数 */
    suspend fun getFollowCount(userId: Long? = null): Map<String, Int> {
        val body = followApi.getFollowCount(userId).okBody().payload()
        return mapOf(
            "fansCount" to body.int("fansCount"),
            "followCount" to body.int("followCount")
        )
    }

    /** 我关注的人（分页） */
    suspend fun getFollowingList(pageNum: Int = 1, pageSize: Int = 20, userId: Long? = null): PageData<FollowUserItem> {
        val table = followApi.getFollowingList(userId, pageNum, pageSize).okTable()
        val list = table.rows.map { row ->
            // 后端返回：userId=我自己、followUserId=被关注人、userName/avatar=被关注人；无 bio 字段
            FollowUserItem(
                userId = row.long("followUserId"),
                userName = row.str("userName"),
                avatar = row.str("avatar"),
                bio = ""
            )
        }
        return PageData(total = table.total, list = list)
    }

    /** 我的粉丝（分页） */
    suspend fun getFansList(pageNum: Int = 1, pageSize: Int = 20, userId: Long? = null): PageData<FollowUserItem> {
        val table = followApi.getFansList(userId, pageNum, pageSize).okTable()
        val list = table.rows.map { row ->
            // 后端返回：userId=粉丝本人、followUserId=我自己、userName/avatar=粉丝；无 bio 字段
            FollowUserItem(
                userId = row.long("userId"),
                userName = row.str("userName"),
                avatar = row.str("avatar"),
                bio = ""
            )
        }
        return PageData(total = table.total, list = list)
    }
}

/** 关注/粉丝列表项 */
data class FollowUserItem(
    val userId: Long = 0,
    val userName: String = "",
    val avatar: String = "",
    val bio: String = ""
)
