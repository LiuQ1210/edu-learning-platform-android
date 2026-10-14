package com.github.learningplatform.data.local

/**
 * 【任务②：定缓存键规则与 TTL；分类树接 Room 缓存】
 *
 * 全局缓存键规范：所有业务缓存 key 统一在此定义，禁止散落在 Repository 里手拼。
 *
 * 命名规则：`业务名:参数1=值1:参数2=值2`（参数按调用顺序拼接）。
 * 硬约束：key 是 cache_kv 表主键，必须保证「同参数必同 key、异参数必异 key」——
 * 拼少了会串数据（不同请求读到同一份缓存），拼多了永远命不中。
 *
 * 当前任务②只落分类树；首页列表（③）、课程详情（④）沿用同一规范，届时在此追加。
 */
object CacheKeys {

    /** 分类树：type=1 文章分类 / type=2 视频课程分类（接口 4.1） */
    const val CATEGORY_TREE = "category_tree:type=%d"

    /** 分类树缓存键，type 与接口 4.1 的入参一致 */
    fun categoryTree(type: Int): String = CATEGORY_TREE.format(type)

    /**
     * 【任务③：首页课程列表接缓存】
     *
     * 课程列表（接口 6.1）。5 个参数必须全部拼进 key，缺一不可：
     * 缺 pageNum 会串页、缺 categoryId 会串分类、缺 sort 会串排序。
     * null 参数用空串占位（与"传了 0"区分开）。
     */
    const val COURSE_HOME = "course_home:page=%d:size=%d:cat=%s:vtype=%s:sort=%s"

    /** 课程列表缓存键，参数与接口 6.1 的入参一一对应 */
    fun courseHome(
        pageNum: Int,
        pageSize: Int,
        categoryId: Long?,
        videoType: Int?,
        sort: Int?
    ): String = COURSE_HOME.format(pageNum, pageSize, categoryId ?: "", videoType ?: "", sort ?: "")

    /**
     * 【任务④：课程详情接缓存 + 失效逻辑（过期重取、下拉强制回源）】
     *
     * 课程详情（接口 6.2），按课程 id 区分，一门课一条缓存。
     * 只有 1 个参数，比任务③的列表 key 简单。
     */
    const val COURSE_DETAIL = "course_detail:%d"

    /** 课程详情缓存键，courseId 与接口 6.2 的入参一致 */
    fun courseDetail(courseId: Long): String = COURSE_DETAIL.format(courseId)
}
