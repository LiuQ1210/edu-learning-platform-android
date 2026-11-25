package com.github.learningplatform.data.remote.dto

/**
 * 智能体接口的解析结果。
 *
 * 后端响应是 `AjaxResult` 平铺字段（没有 `data` 包一层），所以这里只放
 * 已经整理好的 Kotlin 对象，JSON 取值全部收敛在 AgentRepository 里。
 */

/**
 * 回答引用。
 *
 * 课程问答给 `course` / `chapter`，社区问答给 `postId` / `postTitle` / `category`，
 * 两套字段不会同时出现，用同一个类承载即可。
 */
data class AgentRef(
    val course: String = "",
    val chapter: String = "",
    val postId: Long = 0,
    val postTitle: String = "",
    val category: String = ""
)

/** 问答结果（/chat 与 /community/ask 共用） */
data class AgentChatResult(
    val sessionId: Long = 0,
    val messageId: Long = 0,
    val answer: String = "",
    val refs: List<AgentRef> = emptyList(),
    val retrieved: Int = 0
)

/** 会话列表项：scene 为 chat（课程问答）或 community（社区问答） */
data class AgentSessionDto(
    val sessionId: Long = 0,
    val title: String = "",
    val scene: String = "chat",
    val lastMessageTime: String = "",
    val createTime: String = ""
)

/** 历史消息：role 为 user / assistant */
data class AgentMessageDto(
    val messageId: Long = 0,
    val role: String = "",
    val content: String = "",
    val refs: List<AgentRef> = emptyList(),
    val createTime: String = ""
)

/** 异步任务（video_summary / ppt） */
data class AgentTaskDto(
    val taskId: Long = 0,
    val taskType: String = "",
    val status: String = STATUS_PENDING,
    val progress: Int = 0,
    val outputJson: String = "",
    val errorMsg: String = ""
) {
    val isDone: Boolean
        get() = status == STATUS_SUCCESS || status == STATUS_FAILED || status == STATUS_CANCELED

    val isSuccess: Boolean
        get() = status == STATUS_SUCCESS

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELED = "CANCELED"
    }
}

/** 视频总结结果（agent-service /internal/video/summary） */
data class AgentVideoSummaryDto(
    val videoId: Long = 0,
    val videoTitle: String = "",
    val courseId: Long = 0,
    val durationMinutes: Int = 0,
    val summary: String = "",
    val transcriptChars: Int = 0,
    val asr: Boolean = false,
    /** true 表示没有字幕、结果退化为课程信息兜底 */
    val fallback: Boolean = false
)

data class AgentPptSectionDto(
    val title: String = "",
    val summary: String = ""
)

/** PPT 大纲（agent-service /internal/ppt/generate 的 outline 字段） */
data class AgentPptOutlineDto(
    val title: String = "",
    val subtitle: String = "",
    val sections: List<AgentPptSectionDto> = emptyList(),
    val highlights: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val closing: String = ""
)

/** PPT 生成结果：可下载的 pptx 地址 + 大纲 */
data class AgentPptResultDto(
    val url: String = "",
    val fileName: String = "",
    val outline: AgentPptOutlineDto = AgentPptOutlineDto()
)
