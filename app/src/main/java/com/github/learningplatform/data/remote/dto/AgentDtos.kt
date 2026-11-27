package com.github.learningplatform.data.remote.dto

/**
 * 智能体接口的解析结果。
 *
 * 后端响应是 `AjaxResult` 平铺字段（没有 `data` 包一层），所以这里只放
 * 已经整理好的 Kotlin 对象，JSON 取值全部收敛在 AgentRepository 里。
 *
 * 契约见后端 `docs/智能体接口契约.md` 与 `ApiV1AgentController` /
 * `ApiV1AgentActController`。
 */

/**
 * 回答引用。
 *
 * 课程问答给 `course` / `chapter`，社区问答给 `postId` / `postTitle` / `category`，
 * `/agent/act` 的 refs 又是 `{course, chapter}` 与 `{postId, postTitle, excerpt}`
 * 两套结构，这里用同一个类承载，靠 [isCourse] / [isPost] 判断。
 */
data class AgentRef(
    val course: String = "",
    val chapter: String = "",
    val postId: Long = 0,
    val postTitle: String = "",
    val category: String = "",
    val excerpt: String = "",
    val title: String = "",
    val courseId: Long = 0,
    val chapterTitle: String = ""
) {
    val isPost: Boolean
        get() = postId > 0 || postTitle.isNotBlank()

    val isCourse: Boolean
        get() = course.isNotBlank() || chapter.isNotBlank() ||
            courseId > 0 || chapterTitle.isNotBlank() || (title.isNotBlank() && !isPost)

    /** 课程名：新版 act refs 用 title，旧版 chat refs 用 course */
    val courseName: String
        get() = course.ifBlank { title }

    /** 章节名：新版 act refs 用 chapterTitle，旧版 chat refs 用 chapter */
    val chapterName: String
        get() = chapter.ifBlank { chapterTitle }
}

/** 模型提议的长期记忆（只在 /agent/chat 首轮出现，需要用户确认） */
data class AgentMemoryCandidate(
    val candidateId: String = "",
    val content: String = "",
    val reason: String = ""
)

/** 问答结果（/chat、/chat/ask 与 /community/ask 共用） */
data class AgentChatResult(
    val sessionId: Long = 0,
    val messageId: Long = 0,
    val answer: String = "",
    val refs: List<AgentRef> = emptyList(),
    val retrieved: Int = 0,
    /** 本轮用户消息附带的图片地址（历史还原用） */
    val images: List<String> = emptyList(),
    val memoryCandidates: List<AgentMemoryCandidate> = emptyList(),
    /** 流式问答的请求 ID，非流式为 0 */
    val requestId: String = ""
)

/** 会话列表项：scene 见契约（act / chat / course / video / community / sql / ppt） */
data class AgentSessionDto(
    val sessionId: Long = 0,
    val title: String = "",
    val scene: String = "chat",
    val lastMessageTime: String = "",
    val createTime: String = ""
)

/** 会话分页：`{total, rows}` */
data class AgentSessionPage(
    val total: Int = 0,
    val items: List<AgentSessionDto> = emptyList()
)

/** 单条执行步骤（learning_agent_step） */
data class AgentTaskStepDto(
    val seq: Int = 0,
    val stepType: String = "",
    val toolName: String = "",
    val argsJson: String = "",
    val observationJson: String = "",
    val status: String = "",
    val errorCode: String = "",
    val elapsedMs: Int? = null
) {
    val isThought: Boolean get() = stepType == "thought"
    val isTool: Boolean get() = stepType == "tool"
    val isObservation: Boolean get() = stepType == "observation"
}

/** 历史消息：role 为 user / assistant */
data class AgentMessageDto(
    val messageId: Long = 0,
    val role: String = "",
    val content: String = "",
    /** text / image_text / table / task */
    val contentType: String = "text",
    val refs: List<AgentRef> = emptyList(),
    /** 用户提问附带的图片（refsJson.images） */
    val images: List<String> = emptyList(),
    /** 结果卡关联的异步任务 ID，没有则为 0 */
    val taskId: Long = 0,
    /** 后端联查出的任务对象，没有则为 null */
    val task: AgentTaskDto? = null,
    /** 结构化结果快照（SQL 表格），JSON 字符串 */
    val resultJson: String = "",
    val createTime: String = ""
)

/** 异步任务（video_summary / video_subtitle / ppt / act） */
data class AgentTaskDto(
    val taskId: Long = 0,
    val taskType: String = "",
    val status: String = STATUS_PENDING,
    val progress: Int = 0,
    val inputJson: String = "",
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

        const val TYPE_VIDEO_SUMMARY = "video_summary"
        const val TYPE_VIDEO_SUBTITLE = "video_subtitle"
        const val TYPE_PPT = "ppt"
        const val TYPE_ACT = "act"
    }
}

/** 提交异步任务后的返回（summary / subtitle / ppt / act 共用） */
data class AgentTaskStart(
    val taskId: Long = 0,
    val status: String = AgentTaskDto.STATUS_PENDING,
    val sessionId: Long = 0,
    val messageId: Long = 0,
    /** act 命中幂等键时为 true，前端直接接管已有任务 */
    val idempotent: Boolean = false
)

/** 通用任务步骤轮询：GET /agent/tasks/{taskId}/steps */
data class AgentTaskSteps(
    val taskId: Long = 0,
    val status: String = AgentTaskDto.STATUS_PENDING,
    val progress: Int = 0,
    val currentStep: String = "",
    val steps: List<AgentTaskStepDto> = emptyList()
)

/** 执行型智能体详情：GET /agent/act/{taskId} */
data class AgentActDetail(
    val task: AgentTaskDto? = null,
    val status: String = AgentTaskDto.STATUS_PENDING,
    val progress: Int = 0,
    val currentStep: String = "",
    val steps: List<AgentTaskStepDto> = emptyList(),
    val answer: String = "",
    val refs: List<AgentRef> = emptyList(),
    val rounds: Int = 0,
    val maxSteps: Int = 0,
    val toolCallCount: Int = 0,
    /** 因步数 / 超时 / 连续失败提前结束，答案可能不完整 */
    val truncated: Boolean = false,
    val degraded: Boolean = false,
    val degradeReason: String = "",
    val errorCode: String = "",
    val errorMsg: String = ""
) {
    val isDone: Boolean
        get() = status == AgentTaskDto.STATUS_SUCCESS ||
            status == AgentTaskDto.STATUS_FAILED ||
            status == AgentTaskDto.STATUS_CANCELED
}

/** 执行型智能体的最终输出（task.outputJson 解析结果，历史还原用） */
data class AgentActOutput(
    val answer: String = "",
    val refs: List<AgentRef> = emptyList(),
    val rounds: Int = 0,
    val maxSteps: Int = 0,
    val toolCallCount: Int = 0,
    val truncated: Boolean = false,
    val degraded: Boolean = false,
    val degradeReason: String = "",
    val errorCode: String = "",
    val errorMsg: String = ""
)

/** SQL 问答结果：表格 + 一句话结论 */
data class AgentSqlResult(
    val answer: String = "",
    val sql: String = "",
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val rowCount: Int = 0,
    val error: Boolean = false,
    val sessionId: Long = 0,
    val messageId: Long = 0
)

/** 长期记忆（用户级，跨会话生效） */
data class AgentMemoryDto(
    val memoryId: Long = 0,
    val content: String = "",
    val source: String = "manual",
    val status: String = "",
    val createTime: String = "",
    val updateTime: String = ""
)

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
    val fallback: Boolean = false,
    /** 内部错误码，前端不展示，便于排查 */
    val asrErrorCode: String = "",
    /** 与总结同一次 ASR 产出的字幕（成功时必带） */
    val subtitleUrl: String = "",
    val subtitleVttUrl: String = "",
    val subtitleFormat: String = "",
    val subtitleSegments: Int = 0
)

/** 视频字幕结果（agent-service /internal/video/subtitle） */
data class AgentSubtitleDto(
    val videoId: Long = 0,
    val videoTitle: String = "",
    val courseId: Long = 0,
    val durationMinutes: Int = 0,
    val subtitleUrl: String = "",
    val subtitleVttUrl: String = "",
    val subtitleFormat: String = "srt",
    val subtitleSegments: Int = 0,
    val transcriptChars: Int = 0,
    /** true 表示命中已生成字幕，不再重跑 ASR */
    val cached: Boolean = false,
    val asr: Boolean = false
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

/** 结构化页：type 为 cover / contents / section / highlights / keyPoints / closing */
data class AgentPptSlideDto(
    val page: Int = 0,
    val type: String = "",
    val title: String = "",
    val bullets: List<String> = emptyList()
)

/** PPT 生成结果：可下载的 pptx 地址 + 结构化页 + 大纲 */
data class AgentPptResultDto(
    val url: String = "",
    val fileName: String = "",
    val pageCount: Int = 0,
    val slides: List<AgentPptSlideDto> = emptyList(),
    val outline: AgentPptOutlineDto = AgentPptOutlineDto(),
    val videoId: Long = 0
)
