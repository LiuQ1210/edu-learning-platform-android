package com.github.learningplatform.ui.axing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 阿星助手专用图标。
 *
 * 与 [com.github.learningplatform.ui.nav.NavIcons] 同一套内联方式：
 * 只补助手界面用到的图形，底栏/通用的图标继续复用 NavIcons。
 */
private fun fillIcon(
    name: String,
    pathData: String,
    fillType: PathFillType = PathFillType.NonZero
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
    pathFillType = fillType
).build()

private fun lineIcon(name: String, pathData: String, width: Float = 1.8f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()

object AxingIcons {

    /** 四角星：阿星的功能标记 */
    val Sparkle: ImageVector = fillIcon(
        "AxingSparkle",
        "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zM11.5 9.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"
    )

    /** 课程摘要：文档 + 放大镜 */
    val Summarize: ImageVector = lineIcon(
        "AxingSummarize",
        "M6 3.8A1.8 1.8 0 0 1 7.8 2h6.4L19 6.8v13.4a1.8 1.8 0 0 1-1.8 1.8H7.8A1.8 1.8 0 0 1 6 20.2zM14 2v5h5M9 11h6M9 14.5h4M15.4 16.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2zM17.3 19.3 19 21"
    )

    /** 视频总结 */
    val VideoSummary: ImageVector = lineIcon(
        "AxingVideoSummary",
        "M4 6.2A2.2 2.2 0 0 1 6.2 4h11.6A2.2 2.2 0 0 1 20 6.2v11.6a2.2 2.2 0 0 1-2.2 2.2H6.2A2.2 2.2 0 0 1 4 17.8zM10.4 9.2l4.8 2.8-4.8 2.8z"
    )

    /** 学习数据：数据库/统计 */
    val DataStack: ImageVector = fillIcon(
        "AxingDataStack",
        "M20 13H4c-1.1 0-2 .9-2 2v3c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-3c0-1.1-.9-2-2-2zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-1.1 0-2 .9-2 2v3c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"
    )

    /** 生成 PPT */
    val Slideshow: ImageVector = lineIcon(
        "AxingSlideshow",
        "M4.2 5.2a2 2 0 0 1 2-2h11.6a2 2 0 0 1 2 2v13.6a2 2 0 0 1-2 2H6.2a2 2 0 0 1-2-2zM10.4 8.4l4.8 3.2-4.8 3.2z"
    )

    /** 帖子问答 */
    val PostAsk: ImageVector = fillIcon(
        "AxingPostAsk",
        "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM7 9h10v2H7V9zm0 4h7v2H7v-2z"
    )

    /** 发送：向上箭头 */
    val Send: ImageVector = fillIcon(
        "AxingSend",
        "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.59 5.58L20 12l-8-8-8 8z"
    )

    val ChevronDown: ImageVector = fillIcon(
        "AxingChevronDown",
        "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"
    )

    val ChevronRight: ImageVector = fillIcon(
        "AxingChevronRight",
        "M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"
    )

    val ArrowUpRight: ImageVector = lineIcon(
        "AxingArrowUpRight",
        "M7 17 17 7M9.5 7H17v7.5"
    )

    /** 全屏演示 */
    val Fullscreen: ImageVector = fillIcon(
        "AxingFullscreen",
        "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"
    )

    /** 导出 */
    val Export: ImageVector = fillIcon(
        "AxingExport",
        "M9 16h6v-6h4l-7-7-7 7h4v6zM5 18h14v2H5z"
    )

    /** 记忆管理：头部轮廓 */
    val Memory: ImageVector = lineIcon(
        "AxingMemory",
        "M12 3.5a4.5 4.5 0 0 0-4.5 4.5v.9A3 3 0 0 0 6.8 15v1.6A2.9 2.9 0 0 0 12 18.7V21M12 3.5A4.5 4.5 0 0 1 16.5 8v.9a3 3 0 0 1 1.7 5.6v1.7a2.9 2.9 0 0 1-6.2 1.5"
    )

    /** 来源文档 */
    val Doc: ImageVector = lineIcon(
        "AxingDoc",
        "M6 3.8A1.8 1.8 0 0 1 7.8 2h6.4L19 6.8v13.4a1.8 1.8 0 0 1-1.8 1.8H7.8A1.8 1.8 0 0 1 6 20.2zM14 2v5h5M9 12h6M9 16h4"
    )

    /** 新对话 */
    val NewChat: ImageVector = lineIcon(
        "AxingNewChat",
        "M4 6.2A2.2 2.2 0 0 1 6.2 4h7.6a2.2 2.2 0 0 1 2.2 2.2v6.6a2.2 2.2 0 0 1-2.2 2.2H9l-5 4zM18.5 9.5v8.9a2.6 2.6 0 0 1-2.6 2.6h-4.4"
    )

    /** 圆形加号（建议卡右侧） */
    val PlusCircle: ImageVector = lineIcon(
        "AxingPlusCircle",
        "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 8v8M8 12h8",
        width = 1.6f
    )

    /** 图片附件 */
    val Attach: ImageVector = fillIcon(
        "AxingAttach",
        "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
    )

    /** 执行任务：闪电 */
    val Act: ImageVector = fillIcon(
        "AxingAct",
        "M13 2 3 14h7l-1 8 10-12h-7l1-8z"
    )

    /** 数据查询：表格 */
    val Table: ImageVector = lineIcon(
        "AxingTable",
        "M4 4.8h16v14.4H4zM4 9.6h16M4 14.4h16M9.6 9.6v9.6M15.2 9.6v9.6",
        width = 1.6f
    )

    /** 视频字幕：带字幕的屏幕 */
    val Subtitle: ImageVector = lineIcon(
        "AxingSubtitle",
        "M3.8 5.6h16.4v12.8H3.8zM7 13.4h4.4M13.6 13.4H17M7 10.4h10",
        width = 1.6f
    )

    /** 停止 / 取消任务 */
    val Stop: ImageVector = fillIcon(
        "AxingStop",
        "M7 7h10v10H7z"
    )

    /** 步骤时间线：对勾 */
    val StepDone: ImageVector = lineIcon(
        "AxingStepDone",
        "M4 12.5 9 17.5 20 6.5",
        width = 2f
    )

    /** 步骤失败 */
    val StepError: ImageVector = lineIcon(
        "AxingStepError",
        "M6 6l12 12M18 6 6 18",
        width = 2f
    )

    /** 步骤进行中：空心圆 */
    val StepPending: ImageVector = lineIcon(
        "AxingStepPending",
        "M12 4.5a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15z",
        width = 1.8f
    )
}
