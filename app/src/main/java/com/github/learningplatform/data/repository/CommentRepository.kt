package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.CommentApi
import com.github.learningplatform.data.remote.dto.CommentDto
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** 通用评论仓库 */
@Singleton
class CommentRepository @Inject constructor(
    private val commentApi: CommentApi
) {

    /** 评论列表（后端返回 TableDataInfo：{total, rows:[一级评论]}） */
    suspend fun list(targetType: String, targetId: Long): List<CommentDto> {
        val root = commentApi.list(targetType, targetId).okBody()
        val rows = root["rows"]?.jsonArray ?: return emptyList()
        return rows.map { o ->
            val obj = o.jsonObject
            CommentDto(
                commentId = obj.long("commentId"),
                userId = obj.long("userId"),
                userName = obj.str("userName"),
                userAvatar = obj.str("avatar"),
                content = obj.str("content"),
                createTime = obj.str("createTime")
            )
        }
    }

    /** 发表评论/回复 */
    suspend fun add(targetType: String, targetId: Long, content: String, parentId: Long? = null): Long {
        val data = commentApi.add(targetType, targetId, content, parentId).okBody().payload()
        return data.long("commentId")
    }

    /** 删除自己的评论（一级会级联删回复） */
    suspend fun remove(commentId: Long) {
        commentApi.remove(commentId).okBody()
    }
}
