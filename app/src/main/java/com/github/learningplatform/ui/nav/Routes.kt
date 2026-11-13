package com.github.learningplatform.ui.nav

/**
 * 全局路由表。
 *
 * 结构分三块：
 *  1) 认证流   —— 独立 NavHost 图（AppNavHost 内层）
 *  2) 主框架   —— MainScaffold 内的 graph("main")，含 4 个底部 Tab
 *                 以及「需要保留底部导航」的子页面
 *  3) 全屏页   —— 播放器 / 签到 / 富文本编辑等，必须脱离主框架
 *
 * 说明：底部导航是否显示，由 AppNavHost 按当前 route 判断（见 Routes.barRoutes），
 * 不再使用嵌套 NavHost，避免出现「子页面把底栏顶掉」的问题。
 */
object Routes {
    // ---- 认证流 ----
    const val LOGIN = "login"
    const val REGISTER = "register"

    // ---- 主框架 ----
    const val MAIN = "main"
    const val HOME = "home"
    const val VIDEO = "video"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"

    // ---- 课程 ----
    const val COURSE_DETAIL = "course_detail/{courseId}"
    const val SEARCH = "search?categoryId={categoryId}&keyword={keyword}&type={type}"
    const val CATEGORY_ALL = "category_all"

    // ---- 社区 ----
    const val ARTICLE_DETAIL = "article_detail/{articleId}"
    const val ARTICLE_PUBLISH = "article_publish"
    const val ARTICLE_EDIT = "article_edit/{articleId}"
    const val TAG_ARTICLES = "tag_articles/{tagId}"
    const val BLOG_HOME = "blog_home/{userId}"

    // ---- 我的 ----
    const val EDIT_PROFILE = "edit_profile"
    const val MY_COURSES = "my_courses"
    const val STUDY_STATS = "study_stats"
    const val MY_FAVORITES = "my_favorites"
    const val MY_LIKES = "my_likes"
    const val MY_DOWNLOADS = "my_downloads"
    const val WATCH_HISTORY = "watch_history"
    const val NOTES = "notes"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    const val TODOS = "todos"
    const val SETTINGS = "settings"
    const val SHIPPING_ADDRESS = "shipping_address"
    const val CHANGE_PASSWORD = "change_password"
    const val FOLLOWING = "following"
    const val FANS = "fans"

    // ---- 通知 ----
    const val NOTIFICATIONS = "notifications"

    // ---- 签到 / 任务中心（全屏）----
    const val CHECKIN = "checkin"
    const val CHECKIN_RECORD = "checkin_record"

    // ---- 积分商城（全屏）----
    const val POINTS_MALL = "points_mall?balance={balance}&todayPoints={todayPoints}"
    const val PRIZE_EXCHANGE = "prize_exchange/{prizeId}?balance={balance}"
    const val PRIZE_EXCHANGE_SUCCESS = "prize_exchange_success/{prizeId}?remaining={remaining}"
    const val EXCHANGE_RECORDS = "exchange_records"

    // ---- 播放（全屏）----
    const val PLAYER = "player/{courseId}?lessonId={lessonId}"

    /** 显示底部导航的 route 集合 */
    val barRoutes: Set<String> = setOf(HOME, VIDEO, COMMUNITY, PROFILE)

    /** 这些页面属于主框架（有底栏，Tab 切换动画共享），其余为全屏页 */
    val mainRoutes: Set<String> = setOf(
        HOME, VIDEO, COMMUNITY, PROFILE,
        COURSE_DETAIL, SEARCH, CATEGORY_ALL,
        ARTICLE_DETAIL, ARTICLE_PUBLISH, ARTICLE_EDIT, TAG_ARTICLES, BLOG_HOME,
        EDIT_PROFILE, MY_COURSES, MY_FAVORITES, MY_LIKES, MY_DOWNLOADS, WATCH_HISTORY,
        NOTES, NOTE_DETAIL, TODOS, SETTINGS, CHANGE_PASSWORD, FOLLOWING, FANS
    )

    /**
     * @param type 1-文章 2-视频课程。从社区搜索框进来传 1，直接落到「文章」页
     */
    fun search(categoryId: Long? = null, keyword: String? = null, type: Int = 2) =
        "search?categoryId=${categoryId ?: 0L}&keyword=${keyword.orEmpty()}&type=$type"

    fun courseDetail(courseId: Long) = "course_detail/$courseId"
    fun articleDetail(articleId: Long) = "article_detail/$articleId"
    fun video(focusId: Long? = null) = "$VIDEO?focusId=${focusId ?: 0L}"
    fun articleEdit(articleId: Long) = "article_edit/$articleId"
    fun noteDetail(noteId: Long) = "note_detail/$noteId"
    fun tagArticles(tagId: Long) = "tag_articles/$tagId"
    fun blogHome(userId: Long, self: Boolean = false) = "blog_home/$userId?self=$self"
    fun player(courseId: Long, lessonId: Long? = null) =
        "player/$courseId?lessonId=${lessonId ?: 0L}"

    /** 积分商城：把任务中心已加载的余额 / 今日任务积分带过去，避免商城再请求一次账户 */
    fun pointsMall(balance: Int, todayPoints: Int) =
        "points_mall?balance=$balance&todayPoints=$todayPoints"

    /** 奖品兑换：带上奖品 id 和余额，确认页直接复用商城已加载的数据 */
    fun prizeExchange(prizeId: String, balance: Int) =
        "prize_exchange/$prizeId?balance=$balance"

    /** 兑换成功：携带奖品 id 与兑换后剩余积分（-1 表示余额未知/已扣光），仅供结果展示 */
    fun prizeExchangeSuccess(prizeId: String, remaining: Int) =
        "prize_exchange_success/$prizeId?remaining=$remaining"
}