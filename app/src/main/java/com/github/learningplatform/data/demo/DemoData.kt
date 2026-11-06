package com.github.learningplatform.data.demo

import com.github.learningplatform.data.remote.dto.AdSlotDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingResultDto
import com.github.learningplatform.data.remote.dto.TodoDto

/**
 * 后端**尚未提供**的功能所用的本地占位数据。
 *
 * 已经接上后端的模块（分类、课程列表/详情、文章列表/详情、评论、个人资料、
 * 收藏/点赞/观看历史、笔记、课程评分）**不要**再往这里加数据 ——
 * 那些模块直接走 Repository/API，客户端不再内置样例。
 *
 * 当前仍在使用本对象的接口只有：
 *  - 他人博客主页（GET /user/blog-home 未提供）
 *  - 我的课程 / 加入退出课程 / 下载记录（未提供）
 *  - 待办（/api/v1/todos 后端未实现）
 *  - 文章标签 / 搜索热词 / 广告位（未提供）
 *
 * 学习数据统计页（StudyStatsScreen）是纯 UI 占位，不读取本对象。
 *
 * 图片用 picsum 的稳定占位图（同一 seed 每次返回同一张），断网时由 NetImage
 * 落到灰底占位，不影响布局检查。
 */
object DemoData {

    private fun cover(seed: String) = "https://picsum.photos/seed/$seed/480/270"

    // ---------------------------------------------------------------- 文章 / 标签

    val articleCategories: List<CategoryNodeDto> = listOf(
        CategoryNodeDto(101, 0, "技术分享", 1, 1),
        CategoryNodeDto(102, 0, "学习心得", 2, 1),
        CategoryNodeDto(103, 0, "项目实战", 3, 1),
        CategoryNodeDto(104, 0, "求职就业", 4, 1),
        CategoryNodeDto(105, 0, "生活随笔", 5, 1)
    )

    private val articleTitles = listOf(
        "Kotlin 协程取消机制完全指南" to "结构化并发为什么必须向上抛 CancellationException",
        "我是如何三个月从零学会 Android 的" to "踩过的坑与有效的学习路径",
        "Compose 重组优化实战" to "把一次列表滚动从 32ms 降到 8ms",
        "后端接口设计中的 10 个常见错误" to "分页、错误码、幂等……",
        "写技术博客这三年，我收获了什么" to "输出倒逼输入的复利",
        "从校园到职场：我的第一份工作复盘" to "前三个月最该做的三件事",
        "MySQL 索引原理与慢查询优化" to "B+ 树、覆盖索引与执行计划",
        "一次线上内存泄漏的排查记录" to "从 OOM 日志到 LeakCanary"
    )

    val articles: List<ArticleDto> = articleTitles.mapIndexed { i, (t, s) ->
        ArticleDto(
            articleId = 5001L + i,
            title = t,
            summary = s,
            coverUrl = if (i % 3 == 0) "" else cover("article$i"),
            authorId = 1001L + (i % 4),
            authorName = listOf("张三", "李四", "王五", "陈老师")[i % 4],
            authorAvatar = "",
            categoryName = articleCategories[i % articleCategories.size].categoryName,
            viewCount = 1200 + i * 430,
            likeCount = 45 + i * 13,
            commentCount = 3 + i,
            status = if (i == 0) 0 else 2,
            publishTime = "2026-09-1${i % 9} 10:30:00"
        )
    }

    fun articlePage(pageNum: Int, pageSize: Int, categoryId: Long? = null, sort: Int? = null): PageData<ArticleDto> {
        var source = articles
        // 占位数据按分类过滤，让点击 tabs 有反馈
        if (categoryId != null) {
            val idx = (categoryId % 10).toInt()
            source = source.filterIndexed { i, _ -> i % 5 == idx % 5 }
            if (source.isEmpty()) source = articles.take(2)
        }
        // 排序：1最新(默认按时间) 2最热(按点赞) 3精华(只取前3篇当精华)
        when (sort) {
            2 -> source = source.sortedByDescending { it.likeCount }
            3 -> source = source.take(3)
        }
        val all = source + source.map { it.copy(articleId = it.articleId + 7000, title = it.title + "（续）") }
        val slice = all.drop((pageNum - 1) * pageSize).take(pageSize)
        return PageData(total = all.size.toLong(), list = slice)
    }

    val articleTags: List<ArticleTagDto> = listOf(
        ArticleTagDto(1, "Android", 128),
        ArticleTagDto(2, "Kotlin", 96),
        ArticleTagDto(3, "Compose", 74),
        ArticleTagDto(4, "后端", 61),
        ArticleTagDto(5, "面试", 55),
        ArticleTagDto(6, "数据库", 40),
        ArticleTagDto(7, "架构", 33),
        ArticleTagDto(8, "效率工具", 21)
    )

    // ---------------------------------------------------------------- 搜索

    val hotWords: List<HotWordDto> = listOf(
        HotWordDto("Machine Learning", 1280),
        HotWordDto("Java", 1130),
        HotWordDto("数据分析", 960),
        HotWordDto("Compose", 720),
        HotWordDto("面试题", 640),
        HotWordDto("Python", 590),
        HotWordDto("Kotlin", 430),
        HotWordDto("英语口语", 310)
    )

    // ---------------------------------------------------------------- 个人中心（后端未提供）

    val myCourses: PageData<MyCourseDto> = PageData(
        total = 3,
        list = listOf(
            MyCourseDto(1001, "Machine Learning 从入门到实战", cover("course0"), "张老师", 62, "2026-09-17 21:10:00"),
            MyCourseDto(1002, "Data Science 数据分析实战", cover("course1"), "李老师", 28, "2026-09-16 20:05:00"),
            MyCourseDto(1005, "产品经理必修课", cover("course4"), "陈老师", 5, "2026-09-14 19:40:00")
        )
    )

    val downloads: PageData<DownloadRecordDto> = PageData(
        total = 2,
        list = listOf(
            DownloadRecordDto(1, 9001, 1, "第一章课件.pdf", "ch1.pdf", 2_400_000, "2026-09-12 10:00:00"),
            DownloadRecordDto(2, 9002, 2, "实战项目源码.zip", "source.zip", 18_600_000, "2026-09-11 15:30:00")
        )
    )

    // ---------------------------------------------------------------- 待办（后端未实现）

    /**
     * 待办样例。
     *
     * 用命名参数：TodoDto 的 description / sourceName / deadline / remindTime
     * 在客户端是「非空 String + 默认空串」（接口 11.1 返回的也是字符串），
     * 「无此项」要传空串而不是 null，否则编译不过。
     */
    val todos: PageData<TodoDto> = PageData(
        total = 5,
        list = listOf(
            TodoDto(
                todoId = 8001, title = "看完《Java 高并发编程》第二章",
                description = "重点是线程池参数与拒绝策略",
                relateType = 2, relateId = 1003, sourceName = "Java 高并发编程",
                deadline = "2026-09-20 23:59:00", remindTime = "2026-09-20 09:00:00",
                priority = 1, status = 0, createTime = "2026-09-12 10:00:00"
            ),
            TodoDto(
                todoId = 8002, title = "整理协程笔记并输出一篇博客",
                relateType = 1, relateId = 5001, sourceName = "Kotlin 协程取消机制完全指南",
                deadline = "2026-09-25 23:59:00",
                priority = 2, status = 0, createTime = "2026-09-13 14:00:00"
            ),
            TodoDto(
                todoId = 8003, title = "刷 20 道算法题", description = "数组与双指针专题",
                deadline = "2026-09-30 23:59:00",
                priority = 3, status = 0, createTime = "2026-09-14 09:00:00"
            ),
            TodoDto(
                todoId = 8004, title = "完成 ML 课程第三章练习",
                relateType = 2, relateId = 1001, sourceName = "Machine Learning 从入门到实战",
                deadline = "2026-09-18 23:59:00",
                priority = 2, status = 1, createTime = "2026-09-10 08:00:00"
            ),
            TodoDto(
                todoId = 8005, title = "更新简历",
                priority = 3, status = 1, createTime = "2026-09-08 08:00:00"
            )
        )
    )

    // ---------------------------------------------------------------- 广告位（未提供）

    val adSlots: List<AdSlotDto> = listOf(
        AdSlotDto("startup_splash", "开屏广告", "TAKU_SLOT_SPLASH_001", 4),
        AdSlotDto("community_home_banner", "社区主页轮播广告", "TAKU_SLOT_COMM_BANNER_001", 1),
        AdSlotDto("video_home_banner", "视频主页轮播广告", "TAKU_SLOT_VIDEO_BANNER_001", 1)
    )

    // ---------------------------------------------------------------- 课程评分（后端未提供，假数据）

    fun ratings(courseId: Long): PageData<RatingDto> = PageData(
        total = 3,
        list = listOf(
            RatingDto(1001, "张三", "", 5.0, "讲得很清楚"),
            RatingDto(1002, "李四", "", 4.5, "例子实用"),
            RatingDto(1003, "王五", "", 4.0, "适合入门")
        )
    )

    fun rateResult(score: Double) = RatingResultDto(averageScore = score, ratingCount = 129)
}
