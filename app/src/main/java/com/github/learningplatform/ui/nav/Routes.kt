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
    const val MY_ARTICLES = "my_articles"
    const val TAG_ARTICLES = "tag_articles/{tagId}"
    const val BLOG_HOME = "blog_home/{userId}"

    // ---- 我的 ----
    const val EDIT_PROFILE = "edit_profile"
    const val MY_COURSES = "my_courses"
    const val STUDY_STATS = "study_stats"
    const val STUDY_PATH = "study_path"
    const val MY_FAVORITES = "my_favorites"
    const val MY_LIKES = "my_likes"
    const val MY_DOWNLOADS = "my_downloads"
    const val WATCH_HISTORY = "watch_history"
    const val NOTES = "notes"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    const val TODOS = "todos"
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "change_password"

    // ---- 签到（全屏）----
    const val CHECKIN = "checkin"
    const val CHECKIN_RECORD = "checkin_record"

    // ---- 播放（全屏）----
    const val PLAYER = "player/{courseId}?lessonId={lessonId}"

    /** 显示底部导航的 route 集合 */
    val barRoutes: Set<String> = setOf(HOME, VIDEO, COMMUNITY, PROFILE)

    /** 这些页面属于主框架（有底栏，Tab 切换动画共享），其余为全屏页 */
    val mainRoutes: Set<String> = setOf(
        HOME, VIDEO, COMMUNITY, PROFILE,
        COURSE_DETAIL, SEARCH, CATEGORY_ALL,
        ARTICLE_DETAIL, ARTICLE_PUBLISH, ARTICLE_EDIT, MY_ARTICLES, TAG_ARTICLES, BLOG_HOME,
        EDIT_PROFILE, MY_COURSES, MY_FAVORITES, MY_LIKES, MY_DOWNLOADS, WATCH_HISTORY,
        NOTES, NOTE_DETAIL, TODOS, SETTINGS, CHANGE_PASSWORD
    )

    /**
     * @param type 1-文章 2-视频课程。从社区搜索框进来传 1，直接落到「文章」页
     */
    fun search(categoryId: Long? = null, keyword: String? = null, type: Int = 2) =
        "search?categoryId=${categoryId ?: 0L}&keyword=${keyword.orEmpty()}&type=$type"

    fun courseDetail(courseId: Long) = "course_detail/$courseId"
    fun articleDetail(articleId: Long) = "article_detail/$articleId"
    fun articleEdit(articleId: Long) = "article_edit/$articleId"
    fun noteDetail(noteId: Long) = "note_detail/$noteId"
    fun tagArticles(tagId: Long) = "tag_articles/$tagId"
    fun blogHome(userId: Long) = "blog_home/$userId"
    fun player(courseId: Long, lessonId: Long? = null) =
        "player/$courseId?lessonId=${lessonId ?: 0L}"
}