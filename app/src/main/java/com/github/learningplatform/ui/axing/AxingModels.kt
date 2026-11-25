package com.github.learningplatform.ui.axing

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.learningplatform.ui.theme.AccentTeal

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
    STUDY("学习数据")
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
    AxingSuggestion("我最近 7 天学了多久？", AxingCapability.STUDY, AxingIcons.DataStack, Color(0xFF22A9B8)),
    AxingSuggestion("把这门课做成一份 PPT", AxingCapability.PPT, AxingIcons.Slideshow, Color(0xFF2E9FD4))
)

/** 历史会话：sessionId 用来拉消息，scene 区分课程问答 / 社区问答 */
data class AxingHistoryItem(
    val sessionId: Long,
    val title: String,
    val meta: String,
    val scene: String = "chat"
)

data class AxingSource(
    val chapter: String,
    val title: String,
    val time: String? = null
)

data class AxingQuote(val title: String, val excerpt: String)

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

sealed interface AxingBlock {
    data class Paragraph(val text: String) : AxingBlock
    data class Bullets(val items: List<String>) : AxingBlock
    data class Sources(val items: List<AxingSource>) : AxingBlock
    data class Quotes(val items: List<AxingQuote>) : AxingBlock
    data class Deck(val deck: AxingDeck) : AxingBlock
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
    text.contains("视频") -> AxingCapability.VIDEO
    text.contains("多久") || text.contains("7 天") || text.contains("学了") -> AxingCapability.STUDY
    text.contains("总结") || text.contains("重点") -> AxingCapability.SUMMARY
    else -> AxingCapability.QA
}

/** 头像资源：素材目录里的阿星立绘，已压缩到 512px */
@DrawableRes
val AxingAvatarRes: Int = com.github.learningplatform.R.drawable.axing_avatar
