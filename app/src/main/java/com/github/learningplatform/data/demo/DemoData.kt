package com.github.learningplatform.data.demo

import com.github.learningplatform.BuildConfig
import com.github.learningplatform.data.remote.dto.AdSlotDto
import com.github.learningplatform.data.remote.dto.AppConfigDto
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinResultDto
import com.github.learningplatform.data.remote.dto.CheckinTaskDto
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.CourseChapterDto
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.CourseLessonDto
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.HotReplyDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingResultDto
import com.github.learningplatform.data.remote.dto.ReplyDto
import com.github.learningplatform.data.remote.dto.SignCalendarDto
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.data.remote.dto.UserProfileDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto

/**
 * UI 预览样例数据。
 *
 * **只在 `BuildConfig.UI_PREVIEW = true` 时被 Repository 使用**，编译期常量，
 * release 构建关掉后整块数据会被 R8 裁掉，不会进生产包。
 *
 * 存在的意义：后端未就绪时，界面必须能被打开、被点、被评审。没有它就只能看到
 * 一个登录页和满屏「网络异常」。这里给出的是**结构真实**的假数据 —— 字段、分页、
 * 层级关系都与接口文档 v3.1 一致，所以列表滚动、分页加载、空状态、详情跳转都能真跑。
 *
 * 图片用 picsum 的稳定占位图（同一 seed 每次返回同一张），断网时会自动落到
 * NetImage 的灰底占位，不影响布局检查。
 */
object DemoData {

    val enabled: Boolean get() = BuildConfig.UI_PREVIEW

    private fun cover(seed: String) = "https://picsum.photos/seed/$seed/480/270"

    // ---------------------------------------------------------------- 分类

    val courseCategories: List<CategoryNodeDto> = listOf(
        CategoryNodeDto(1, 0, "艺术", 1, 1, "", listOf(
            CategoryNodeDto(11, 1, "绘画", 1, 1),
            CategoryNodeDto(12, 1, "音乐", 2, 1),
            CategoryNodeDto(13, 1, "设计鉴赏", 3, 1)
        )),
        CategoryNodeDto(2, 0, "商业", 2, 1, "", listOf(
            CategoryNodeDto(21, 2, "市场营销", 1, 1),
            CategoryNodeDto(22, 2, "财务管理", 2, 1),
            CategoryNodeDto(23, 2, "商业分析", 3, 1)
        )),
        CategoryNodeDto(3, 0, "IT 解决方案", 3, 1, "", listOf(
            CategoryNodeDto(31, 3, "Java 开发", 1, 1),
            CategoryNodeDto(32, 3, "Python 编程", 2, 1),
            CategoryNodeDto(33, 3, "大数据", 3, 1),
            CategoryNodeDto(34, 3, "前端开发", 4, 1)
        )),
        CategoryNodeDto(4, 0, "语言学习", 4, 1, "", listOf(
            CategoryNodeDto(41, 4, "英语", 1, 1),
            CategoryNodeDto(42, 4, "日语", 2, 1)
        )),
        CategoryNodeDto(5, 0, "职业发展", 5, 1, ""),
        CategoryNodeDto(6, 0, "考试认证", 6, 1, "")
    )

    val articleCategories: List<CategoryNodeDto> = listOf(
        CategoryNodeDto(101, 0, "技术分享", 1, 1),
        CategoryNodeDto(102, 0, "学习心得", 2, 1),
        CategoryNodeDto(103, 0, "项目实战", 3, 1),
        CategoryNodeDto(104, 0, "求职就业", 4, 1),
        CategoryNodeDto(105, 0, "生活随笔", 5, 1)
    )

    /**
     * 按接口 4.1 的 type 取分类树。
     * @param type 1-文章分类 2-视频课程分类
     */
    fun categoryTree(type: Int): List<CategoryNodeDto> =
        if (type == 1) articleCategories else courseCategories

    // ---------------------------------------------------------------- 课程

    private val courseTitles = listOf(
        "Machine Learning 从入门到实战" to "深度学习与神经网络基础",
        "Data Science 数据分析实战" to "Python + Pandas 全流程",
        "Java 高并发编程" to "从 JMM 到线程池调优",
        "Android 性能优化" to "启动、内存、包体积三板斧",
        "产品经理必修课" to "从需求洞察到 PRD",
        "UI 设计基础" to "色彩、排版与组件规范",
        "英语口语突破" to "场景化口语训练",
        "财务分析入门" to "读懂三张报表",
        "Kubernetes 实战" to "集群部署与运维",
        "算法与数据结构" to "面试高频题型精讲"
    )

    val shortVideos: List<CourseDto> = courseTitles.take(6).mapIndexed { i, (t, _) ->
        CourseDto(
            courseId = 2001L + i,
            title = t,
            coverUrl = cover("short$i"),
            videoType = 1,
            duration = 45 + i * 12,
            instructorName = listOf("张老师", "李老师", "王老师", "陈老师")[i % 4],
            averageScore = 4.5 + (i % 5) * 0.1,
            studentCount = 820 + i * 340,
            viewCount = 12000 + i * 2100
        )
    }

    val courses: List<CourseDto> = courseTitles.mapIndexed { i, (t, _) ->
        CourseDto(
            courseId = 1001L + i,
            title = t,
            coverUrl = cover("course$i"),
            videoType = 2,
            duration = 3600 + i * 420,
            instructorName = listOf("张老师", "李老师", "王老师", "陈老师", "刘老师")[i % 5],
            averageScore = 4.4 + (i % 6) * 0.1,
            studentCount = 1200 + i * 560,
            viewCount = 5600 + i * 1300
        )
    }

    fun coursePage(pageNum: Int, pageSize: Int, videoType: Int?): PageData<CourseDto> {
        val source = if (videoType == 1) shortVideos else courses
        // 造两页就够，用来验证上拉加载与「没有更多了」
        val all = source + source.map { it.copy(courseId = it.courseId + 5000, title = it.title + "（进阶）") }
        val from = (pageNum - 1) * pageSize
        val slice = all.drop(from).take(pageSize)
        return PageData(total = all.size.toLong(), list = slice)
    }

    fun courseDetail(courseId: Long): CourseDetailDto {
        val base = (courses + shortVideos).firstOrNull { it.courseId == courseId }
            ?: courses.first().copy(courseId = courseId)
        val isShort = base.videoType == 1
        val chapters = if (isShort) emptyList() else listOf(
            CourseChapterDto(3001, "第一章：环境搭建", listOf(
                CourseLessonDto(4001, "1.1 开发环境准备", 536, 1, 1, 0, 1),
                CourseLessonDto(4002, "1.2 第一个程序", 720, 0, 1, 120, 0),
                CourseLessonDto(4003, "1.3 常见问题排查", 420, 0, 1, 0, 0)
            )),
            CourseChapterDto(3002, "第二章：核心概念", listOf(
                CourseLessonDto(4004, "2.1 数据结构概览", 900, 0, 1, 0, 0),
                CourseLessonDto(4005, "2.2 算法复杂度", 1080, 0, 1, 0, 0)
            )),
            CourseChapterDto(3003, "第三章：项目实战", listOf(
                CourseLessonDto(4006, "3.1 需求拆解", 660, 0, 1, 0, 0),
                CourseLessonDto(4007, "3.2 编码实现", 1500, 0, 1, 0, 0)
            ))
        )
        return CourseDetailDto(
            courseId = base.courseId,
            title = base.title,
            coverUrl = base.coverUrl,
            videoType = base.videoType,
            description = "本课程从零开始，带你完整走一遍核心知识体系。每个章节都配有实操演示与课后练习，" +
                "覆盖常见坑点与最佳实践，学完可以直接上手真实项目。",
            instructorName = base.instructorName,
            duration = base.duration,
            totalLessons = chapters.sumOf { it.lessons.size },
            averageScore = base.averageScore,
            ratingCount = 128,
            studentCount = base.studentCount,
            viewCount = base.viewCount,
            isFree = if (isShort) 1 else 0,
            // 让第一章第一节免费试看，用来验证「未加入也能试看」这条规则
            isJoined = false,
            chapters = chapters
        )
    }

    fun ratings(courseId: Long): PageData<RatingDto> = PageData(
        total = 3,
        list = listOf(
            RatingDto(1001, "张三", "", 5.0, "2026-09-10 10:00:00"),
            RatingDto(1002, "李四", "", 4.5, "2026-09-09 18:20:00"),
            RatingDto(1003, "王五", "", 4.0, "2026-09-08 09:05:00")
        )
    )

    fun playInfo(courseId: Long, lessonId: Long?) = PlayInfoDto(
        courseId = courseId,
        lessonId = lessonId ?: 0L,
        // 公开测试流，播放器有真实内容可播
        playUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        expireTime = System.currentTimeMillis() / 1000 + 3600,
        watermarkText = "DemoUser1001",
        duration = 596
    )

    fun rateResult(score: Double) = RatingResultDto(averageScore = score, ratingCount = 129)

    // ---------------------------------------------------------------- 文章

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

    fun articlePage(pageNum: Int, pageSize: Int): PageData<ArticleDto> {
        val all = articles + articles.map { it.copy(articleId = it.articleId + 7000, title = it.title + "（续）") }
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

    fun articleDetail(articleId: Long): ArticleDetailDto {
        val base = articles.firstOrNull { it.articleId == articleId } ?: articles.first()
        return ArticleDetailDto(
            articleId = base.articleId,
            title = base.title,
            summary = base.summary,
            content = """
                <p>这是 UI 预览模式下的<b>样例正文</b>，用来验证富文本渲染、图片宽度、表格与代码块。</p>
                <h3>一、背景</h3>
                <p>${base.summary}。正文由后端返回 HTML，客户端用 WebView 渲染并注入适配样式。</p>
                <p><img src="${cover("body${base.articleId}")}" alt="示例图"/></p>
                <h3>二、要点</h3>
                <ul>
                  <li>段落、标题、列表、引用都要能正常排版</li>
                  <li>图片不得超过屏幕宽度</li>
                  <li>代码块要可横向滚动</li>
                </ul>
                <blockquote>引用样式：好的接口文档能省掉一半的联调会议。</blockquote>
                <pre><code>val page = repo.getArticles(pageNum = 1, sort = 2)</code></pre>
                <table>
                  <tr><th>参数</th><th>说明</th></tr>
                  <tr><td>pageNum</td><td>页码，默认 1</td></tr>
                  <tr><td>sort</td><td>1 最新 / 2 最热 / 3 精华</td></tr>
                </table>
                <p>正文结束。</p>
            """.trimIndent(),
            coverUrl = base.coverUrl,
            authorId = base.authorId,
            authorName = base.authorName,
            authorAvatar = "",
            categoryId = 101,
            categoryName = base.categoryName,
            tags = articleTags.take(3),
            viewCount = base.viewCount,
            likeCount = base.likeCount,
            commentCount = base.commentCount,
            favoriteCount = 12,
            isLiked = false,
            isFavorited = false,
            publishTime = base.publishTime
        )
    }

    fun myArticles(status: Int?): PageData<ArticleDto> {
        val list = articles.filter { status == null || it.status == status }
        return PageData(total = list.size.toLong(), list = list)
    }

    // ---------------------------------------------------------------- 评论

    /**
     * 评论全量列表（不含分页）。
     *
     * 评论走**游标分页**，所以这里只提供完整列表，由 Repository 按 cursor 切页。
     * 不要在这里做「按页码切片」—— 那套只适合 offset 分页，用在本接口上会让
     * 每次调用返回同一页，客户端累加后产生重复 commentId 并崩溃。
     *
     * commentId 必须互不相同：LazyColumn 用的是 `key = { it.commentId }`。
     */
    fun commentList(targetId: Long): List<CommentDto> = listOf(
        CommentDto(
            88009, 1001, "张三", "", "讲得很清晰，第一章就解决了我卡了很久的环境问题。",
            12, 2, false, "2026-09-12 10:00:00",
            listOf(HotReplyDto(88010, "李四", "张三", "同感，1.3 那节特别有用"))
        ),
        CommentDto(88011, 1002, "李四", "", "希望能补充一下复杂度的推导过程。", 5, 0, true, "2026-09-12 11:20:00"),
        CommentDto(
            88012, 1003, "王五", "", "已加入学习，进度到第二章了。", 2, 1, false, "2026-09-12 13:05:00",
            listOf(HotReplyDto(88013, "张三", "王五", "加油，第三章是重点"))
        ),
        CommentDto(88020, 1004, "陈老师", "", "有疑问可以在评论区留言，我会定期回复。", 30, 0, false, "2026-09-13 09:00:00"),
        CommentDto(88021, 1005, "赵六", "", "第六章的示例代码跑不通，是版本问题吗？", 8, 0, false, "2026-09-13 15:40:00"),
        CommentDto(88022, 1006, "孙七", "", "已看完，准备二刷。", 1, 0, false, "2026-09-14 08:15:00")
    )

    fun replies(rootId: Long): List<ReplyDto> = listOf(
        ReplyDto(88030, 1002, "李四", "", 1001, "张三", "确实，我照着做一遍就通了。", 3, false, "2026-09-12 10:30:00"),
        ReplyDto(88031, 1003, "王五", "", 1001, "张三", "感谢分享！", 0, false, "2026-09-12 10:45:00"),
        ReplyDto(88032, 1001, "张三", "", 1002, "李四", "1.3 那节我看了两遍才通。", 0, false, "2026-09-12 11:02:00")
    )

    // ---------------------------------------------------------------- 搜索 / 分类内容

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

    fun searchResult(keyword: String?, type: Int, pageNum: Int): PageData<ContentItemDto> {
        val items = if (type == 1) {
            articles.map {
                ContentItemDto(
                    id = it.articleId, type = 1, title = it.title, summary = it.summary,
                    coverUrl = it.coverUrl, categoryId = 101, categoryName = it.categoryName,
                    authorId = it.authorId, authorName = it.authorName,
                    viewCount = it.viewCount, likeCount = it.likeCount, commentCount = it.commentCount,
                    createTime = it.publishTime, publishTime = it.publishTime
                )
            }
        } else {
            courses.map {
                ContentItemDto(
                    id = it.courseId, type = 2, title = it.title, summary = "系统讲解 + 实战演练",
                    coverUrl = it.coverUrl, categoryId = 3, categoryName = "IT 解决方案",
                    authorId = 88, authorName = it.instructorName,
                    viewCount = it.viewCount,
                    duration = it.duration, score = it.averageScore,
                    publishTime = "2026-09-01 10:00:00"
                )
            }
        }
        val filtered = if (keyword.isNullOrBlank()) items
        else items.filter { it.title.contains(keyword, ignoreCase = true) }
            .ifEmpty { items }   // 预览模式下不制造「无结果」，避免看起来像坏了
        val slice = filtered.drop((pageNum - 1) * 10).take(10)
        return PageData(total = filtered.size.toLong(), list = slice)
    }

    // ---------------------------------------------------------------- 个人中心

    val profile = UserProfileDto(
        userId = 1001,
        username = "user_1001",
        nickname = "张三",
        avatar = "",
        phone = "138****0000",
        email = "zhangsan@example.com",
        gender = 1,
        bio = "热爱学习，正在补 Android 和算法。",
        continueSignDay = 7,
        lastSignDate = "2026-09-17",
        createTime = "2026-01-01 00:00:00"
    )

    val myCourses: PageData<MyCourseDto> = PageData(
        total = 3,
        list = listOf(
            MyCourseDto(1001, "Machine Learning 从入门到实战", cover("course0"), "张老师", 62, "2026-09-17 21:10:00"),
            MyCourseDto(1002, "Data Science 数据分析实战", cover("course1"), "李老师", 28, "2026-09-16 20:05:00"),
            MyCourseDto(1005, "产品经理必修课", cover("course4"), "陈老师", 5, "2026-09-14 19:40:00")
        )
    )

    val favorites: PageData<UserTargetItemDto> = PageData(
        total = 4,
        list = listOf(
            UserTargetItemDto(1, 5001, "Kotlin 协程取消机制完全指南", cover("article0"), "2026-09-10 10:00:00"),
            UserTargetItemDto(2, 1003, "Java 高并发编程", cover("course2"), "2026-09-09 09:00:00"),
            UserTargetItemDto(1, 5004, "后端接口设计中的 10 个常见错误", cover("article3"), "2026-09-08 21:00:00"),
            UserTargetItemDto(2, 2001, "Machine Learning 从入门到实战", cover("short0"), "2026-09-07 18:00:00")
        )
    )

    /**
     * 我的点赞。
     *
     * 单独一份而不是复用 [favorites]：两者数据不该相同 ——
     * 点赞和收藏是独立操作，用户可能只点赞不收藏。
     * 复用会让「点赞页和收藏页一模一样」，评审时看不出问题，
     * 真接后端才会发现字段映射接错（getLikes 接到了收藏接口）。
     */
    val likes: PageData<UserTargetItemDto> = PageData(
        total = 5,
        list = listOf(
            UserTargetItemDto(2, 1001, "Machine Learning 从入门到实战", cover("course0"), "2026-09-16 20:10:00"),
            UserTargetItemDto(1, 5002, "结构化并发为什么必须向上抛 CancellationException", cover("article1"), "2026-09-15 22:40:00"),
            UserTargetItemDto(2, 2002, "Data Science 数据分析实战", cover("short1"), "2026-09-14 19:05:00"),
            UserTargetItemDto(1, 5003, "Compose 重组优化实战", cover("article2"), "2026-09-13 11:20:00"),
            UserTargetItemDto(2, 1004, "Android 性能优化", cover("course3"), "2026-09-12 08:45:00")
        )
    )

    val history: PageData<ViewHistoryItemDto> = PageData(
        total = 4,
        list = listOf(
            ViewHistoryItemDto(2, 1001, "Machine Learning 从入门到实战", cover("course0"), 1320, "2026-09-17 21:10:00"),
            ViewHistoryItemDto(1, 5001, "Kotlin 协程取消机制完全指南", cover("article0"), 0, "2026-09-16 22:30:00"),
            ViewHistoryItemDto(2, 2002, "Data Science 数据分析实战", cover("short1"), 480, "2026-09-15 20:15:00"),
            ViewHistoryItemDto(1, 5003, "Compose 重组优化实战", cover("article2"), 0, "2026-09-14 09:50:00")
        )
    )

    val downloads: PageData<DownloadRecordDto> = PageData(
        total = 2,
        list = listOf(
            DownloadRecordDto(1, 9001, 1, "第一章课件.pdf", "ch1.pdf", 2_400_000, "2026-09-12 10:00:00"),
            DownloadRecordDto(2, 9002, 2, "实战项目源码.zip", "source.zip", 18_600_000, "2026-09-11 15:30:00")
        )
    )

    val signCalendar = SignCalendarDto(
        continueSignDay = 7,
        totalSignDay = 20,
        signedDates = (1..17).map { "2026-09-%02d".format(it) }
    )

    // ---------------------------------------------------------------- 笔记 / 待办

    val notes: PageData<NoteDto> = PageData(
        total = 4,
        list = listOf(
            NoteDto(7001, "协程取消的三个关键点", "1) 取消是协作式的\n2) 挂起点才会检查取消状态\n3) CancellationException 必须重新抛出",
                1, 5001, "Kotlin 协程取消机制完全指南", cover("article0"), null, "2026-09-17 21:30:00"),
            NoteDto(7002, "JMM 与可见性", "volatile 保证可见性与有序性，不保证原子性。synchronized 三者都保证。",
                2, 1003, "Java 高并发编程", cover("course2"), 1320, "2026-09-16 20:10:00"),
            NoteDto(7003, "组合优于继承的实例", "用策略模式替换掉三层继承，测试用例从 12 个减到 4 个。",
                2, 1002, "Data Science 数据分析实战", cover("course1"), 480, "2026-09-15 19:00:00"),
            NoteDto(7004, "接口设计笔记", "分页统一用 pageNum/pageSize；写操作一律带幂等 Token。",
                1, 5004, "后端接口设计中的 10 个常见错误", cover("article3"), null, "2026-09-13 11:20:00")
        )
    )

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

    // ---------------------------------------------------------------- 签到

    val checkinTasks: List<CheckinTaskDto> = listOf(
        CheckinTaskDto(6001, "每日基地打卡", 1, "点击即可完成今日签到",
            null, null, 100, null, "2026-09-18 00:00:00", "2026-09-18 23:59:59", 0),
        CheckinTaskDto(6002, "实训室位置签到", 2, "请在实训楼 3 楼范围内签到",
            106.630, 26.647, 500, null, "2026-09-18 08:00:00", "2026-09-18 18:00:00", 1),
        CheckinTaskDto(6003, "手势签到", 3, "绘制管理员预设的九宫格图案",
            null, null, 100, "0,1,2,5,8", "2026-09-18 08:00:00", "2026-09-18 20:00:00", 0),
        CheckinTaskDto(6004, "现场拍照签到", 4, "上传一张现场照片作为凭证",
            null, null, 100, null, "2026-09-18 08:00:00", "2026-09-18 22:00:00", 0)
    )

    val checkinRecords: PageData<CheckinRecordDto> = PageData(
        total = 4,
        list = listOf(
            CheckinRecordDto(9001, 6001, "每日基地打卡", 1, "2026-09-17 08:12:00", 1, ""),
            CheckinRecordDto(9002, 6002, "实训室位置签到", 2, "2026-09-16 09:30:00", 1, ""),
            CheckinRecordDto(9003, 6003, "手势签到", 3, "2026-09-15 10:05:00", 0, "手势不匹配"),
            CheckinRecordDto(9004, 6001, "每日基地打卡", 1, "2026-09-14 07:58:00", 1, "")
        )
    )

    fun checkinResult(taskName: String) = CheckinResultDto(
        checkinStatus = 1,
        message = "签到成功，连续签到 8 天",
        continueSignDay = 8,
        checkinTime = "2026-09-18 09:18:00"
    )

    // ---------------------------------------------------------------- 配置 / 广告

    val adSlots: List<AdSlotDto> = listOf(
        AdSlotDto("startup_splash", "开屏广告", "TAKU_SLOT_SPLASH_001", 4),
        AdSlotDto("community_home_banner", "社区主页轮播广告", "TAKU_SLOT_COMM_BANNER_001", 1),
        AdSlotDto("video_home_banner", "视频主页轮播广告", "TAKU_SLOT_VIDEO_BANNER_001", 1)
    )

    val appConfig = AppConfigDto(
        appTitle = "智能学习平台",
        communityAuditEnabled = true,
        videoFreePreviewSeconds = 180,
        adEnabled = true
    )
}
