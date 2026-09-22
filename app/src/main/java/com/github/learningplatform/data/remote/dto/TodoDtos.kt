package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 11. 待办模块 ----------------------

@Serializable
data class TodoDto(
    val todoId: Long,
    val title: String = "",
    val description: String = "",
    /** 关联资源类型 1文章 2视频 */
    val relateType: Int? = null,
    val relateId: Long? = null,
    val sourceName: String = "",
    val deadline: String = "",
    val remindTime: String = "",
    /** 1高 2中 3低 */
    val priority: Int = 2,
    /** 0-未完成 1-已完成 */
    val status: Int = 0,
    val createTime: String = ""
)

@Serializable
data class CreateTodoRequest(
    val title: String,
    val description: String? = null,
    val relateType: Int? = null,
    val relateId: Long? = null,
    val deadline: String? = null,
    val remindTime: String? = null,
    val priority: Int? = null
)

@Serializable
data class UpdateTodoRequest(
    val title: String? = null,
    val description: String? = null,
    val relateType: Int? = null,
    val relateId: Long? = null,
    val deadline: String? = null,
    val remindTime: String? = null,
    val priority: Int? = null
)

@Serializable
data class CreateTodoResponse(val todoId: Long)

@Serializable
data class TodoStatusRequest(val status: Int)