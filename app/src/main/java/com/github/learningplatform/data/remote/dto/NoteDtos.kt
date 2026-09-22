package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 10. 笔记模块 ----------------------

@Serializable
data class NoteDto(
    val noteId: Long,
    /** v3.0 新增标题字段 */
    val title: String = "",
    val content: String = "",
    /** 1-文章 2-视频/课程 */
    val sourceType: Int = 1,
    val sourceId: Long = 0,
    val sourceTitle: String = "",
    val sourceCover: String = "",
    /** 视频笔记对应的播放进度（秒） */
    val videoTimestamp: Int? = null,
    val updateTime: String = ""
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String,
    val sourceType: Int,
    val sourceId: Long,
    val videoTimestamp: Int? = null
)

@Serializable
data class CreateNoteResponse(val noteId: Long)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null
)