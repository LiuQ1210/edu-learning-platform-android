package com.github.learningplatform.ui.axing

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.learningplatform.ui.theme.AccentTeal
import com.github.learningplatform.ui.theme.Primary

/**
 * 阿星助手的界面模型。
 *
 * 这些类只描述「怎么展示」，数据全部来自 `/api/v1/agent` 与学习数据接口，
 * 页面里不再保留任何写死的演示回答。
 */

enum class AxingCapability(val label: String) {
    SUMMARY("课程摘要"),
    QA("课程问答"),
    VIDEO("视频总结"),
    PPT("生成 PPT"),
    STUDY("学习数据"),
    ACT("执行任务"),
    DATA("数据查询"),
    SUBTITLE("视频字幕")
}

/** 课程上下文：id 传给 `/agent/chat` 的 courseId，subtitle 仅用于选择弹层 */
data class AxingCourse(
    val id: Long,
    val title: String,
    val subtitle: String = ""
)

data class AxingSuggestion(
    val text: String,
    val capability: AxingCapability,
    val icon: ImageVector,
    val tint: Color
)

val AxingSuggestions = listOf(
    AxingSuggestion("总结一门课，列出重点", AxingCapability.SUMMARY, AxingIcons.Summarize, Color(0xFF3677EF)),
    AxingSuggestion("这条视频讲了什么？", AxingCapability.VIDEO, AxingIcons.VideoSummary, AccentTeal),
    AxingSuggestion("帮我总结这门课并做成一份 PPT", AxingCapability.ACT, AxingIcons.Act, Color(0xFF7A5AF8)),
    AxingSuggestion("平台一共有多少门课程？", AxingCapability.DATA, AxingIcons.Table, Color(0xFF22A9B8))
)

/** 历史会话：sessionId 用来拉消息，scene 区分课程 / 社区 / 执行任务等场景 */
data class AxingHistoryItem(
    val sessionId: Long,
    val title: String,
    val meta: String,
    val scene: String = "chat"
)

/** 会话场景文案：不在表内的值按 chat 兜底（契约要求） */
fun sceneLabel(scene: String): String = when (scene) {
    "course" -> "课程问答"
    "community" -> "社区问答"
    "sql" -> "数据查询"
    "ppt" -> "PPT 生成"
    "video" -> "视频总结"
    "act" -> "执行任务"
    else -> "自由对话"
}

/** 会话场景图标：与 [sceneLabel] 同一张表，未知场景按 chat 兜底 */
fun sceneIcon(scene: String): ImageVector = when (scene) {
    "course" -> AxingIcons.Summarize
    "community" -> AxingIcons.PostAsk
    "sql" -> AxingIcons.Table
    "ppt" -> AxingIcons.Slideshow
    "video" -> AxingIcons.VideoSummary
    "act" -> AxingIcons.Act
    else -> AxingIcons.PostAsk
}

/** 会话场景配色：与能力胶囊一致 */
fun sceneTint(scene: String): Color = when (scene) {
    "course", "chat" -> Primary
    "community" -> Primary
    "sql" -> Color(0xFF1D9B8F)
    "ppt" -> Color(0xFF2E9FD4)
    "video" -> Color(0xFF2AA7B8)
    "act" -> Color(0xFF7A5AF8)
    else -> Primary
}

data class AxingSource(
    val chapter: String,
    val title: String,
    val time: String? = null
)

data class AxingQuote(
    val title: String,
    val excerpt: String
)

data class AxingSlide(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val bullets: List<String>,
    val footer: String
)

data class AxingDeck(
    val title: String,
    val courseTitle: String,
    val slides: List<AxingSlide>,
    /** 后端生成的 pptx 下载地址；为空时导出退化为文本分享 */
    val fileUrl: String = "",
    val fileName: String = ""
)

/** 执行型智能体的一步：thought / tool / observation */
data class AxingStepUi(
    val seq: Int,
    val kind: String,
    val title: String,
    val detail: String,
    val status: String = "",
    val elapsedText: String = ""
) {
    val isError: Boolean get() = status == "error"
}

/** SQL 结果表：headers + rows，最多 20 行（后端快照口径） */
data class AxingTable(
    val headers: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int,
    val sql: String = "",
    val error: Boolean = false
)

/** 视频字幕卡：SRT 直链 + 可挂 `<track>` 的 VTT */
data class AxingSubtitle(
    val title: String,
    val srtUrl: String,
    val vttUrl: String,
    val format: String = "srt",
    val segments: Int = 0,
    val cached: Boolean = false
)

/** 历史里的任务结果卡（PPT / 视频 / 执行任务）状态 */
data class AxingTaskInfo(
    val title: String,
    val status: String,
    val detail: String = "",
    val errorMsg: String = ""
) {
    val pending: Boolean get() = status == "PENDING" || status == "RUNNING"
    val failed: Boolean get() = status == "FAILED"
    val canceled: Boolean get() = status == "CANCELED"
}

/** 模型提议的长期记忆：展示「保存 / 忽略」 */
data class AxingMemoryOption(
    val candidateId: String,
    val content: String,
    val reason: String = ""
)

sealed interface AxingBlock {
    data class Paragraph(val text: String) : AxingBlock
    data class Bullets(val items: List<String>) : AxingBlock
    data class Sources(val items: List<AxingSource>) : AxingBlock
    data class Quotes(val items: List<AxingQuote>) : AxingBlock
    data class Deck(val deck: AxingDeck) : AxingBlock

    /** 执行步骤时间线；note 是降级/截断说明 */
    data class Steps(val items: List<AxingStepUi>, val note: String? = null) : AxingBlock
    data class Table(val table: AxingTable) : AxingBlock
    data class SubtitleCard(val subtitle: AxingSubtitle) : AxingBlock
    data class TaskCard(val info: AxingTaskInfo) : AxingBlock
    data class MemoryOptions(val items: List<AxingMemoryOption>) : AxingBlock
}

data class AxingAnswer(
    val capability: AxingCapability,
    val course: AxingCourse?,
    val heading: String?,
    val blocks: List<AxingBlock>,
    val caption: String? = null
)

sealed interface AxingMessage {
    data class User(val text: String, val imageUri: String? = null) : AxingMessage
    data class Assistant(val answer: AxingAnswer) : AxingMessage

    /** 请求失败：保留错误原因，页面提供重试 */
    data class Error(val text: String) : AxingMessage
}

/** 帖子内问阿星的回答 */
data class AxingPostAnswer(
    val paragraphs: List<String>,
    val quotes: List<AxingQuote>
)

sealed interface AxingPostMessage {
    data class User(val text: String) : AxingPostMessage
    data class Answer(val content: AxingPostAnswer) : AxingPostMessage
    data class Error(val text: String) : AxingPostMessage
}

/** 没选能力时按关键词猜一个，猜不到就走课程问答 */
fun inferCapability(text: String): AxingCapability = when {
    text.contains("PPT", ignoreCase = true) || text.contains("幻灯片") -> AxingCapability.PPT
    text.contains("字幕") -> AxingCapability.SUBTITLE
    text.contains("视频") -> AxingCapability.VIDEO
    text.contains("多久") || text.contains("7 天") || text.contains("学了") -> AxingCapability.STUDY
    text.contains("多少") || text.contains("统计") || text.contains("人数") ||
        text.contains("排名") -> AxingCapability.DATA
    text.contains("帮我") && (text.contains("并") || text.contains("然后") ||
        text.contains("顺便")) -> AxingCapability.ACT
    text.contains("执行") || text.contains("规划") -> AxingCapability.ACT
    text.contains("总结") || text.contains("重点") -> AxingCapability.SUMMARY
    else -> AxingCapability.QA
}

/** 头像资源：素材目录里的阿星立绘，已压缩到 512px */
@DrawableRes
val AxingAvatarRes: Int = com.github.learningplatform.R.drawable.axing_avatar

/** 各能力的图标与主色，输入栏与回答卡共用 */
fun capabilityIcon(capability: AxingCapability): Pair<ImageVector, Color> = when (capability) {
    AxingCapability.SUMMARY -> AxingIcons.Summarize to Primary
    AxingCapability.QA -> AxingIcons.PostAsk to Primary
    AxingCapability.VIDEO -> AxingIcons.VideoSummary to Color(0xFF2AA7B8)
    AxingCapability.PPT -> AxingIcons.Slideshow to Color(0xFF2E9FD4)
    AxingCapability.STUDY -> AxingIcons.DataStack to Color(0xFF22A9B8)
    AxingCapability.ACT -> AxingIcons.Act to Color(0xFF7A5AF8)
    AxingCapability.DATA -> AxingIcons.Table to Color(0xFF1D9B8F)
    AxingCapability.SUBTITLE -> AxingIcons.Subtitle to Color(0xFFB4671F)
}
