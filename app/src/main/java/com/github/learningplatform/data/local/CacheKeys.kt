package com.github.learningplatform.data.local

/**
 * 缓存键。
 *
 * 约定：
 *  - 全部用 `模块:标识` 的形式，冒号分隔，便于按前缀清理
 *  - **必须带上影响结果的所有参数**（type、categoryId、pageNum…），
 *    漏参数会导致不同请求命中同一份缓存，是最容易犯也最难查的错
 *  - 参数缺失用固定字面量（如 `all`）而不是空串，便于肉眼排查
 *
 * 键一旦发布就别改语义——用户升级后旧键会变成垃圾数据。
 * 结构变化时改前缀版本号（如 `v2:`），别复用旧键。
 *
 * 【任务②：定缓存键规则与 TTL；分类树接 Room 缓存】
 * 【任务③：首页课程列表接缓存】
 * 【任务④：课程详情接缓存 + 失效逻辑（过期重取、下拉强制回源）】
 */
object CacheKeys {
    private const val V = "v1"

    /**
     * 分页列表。模块名 + 所有查询参数。
     * 只缓存第 1 页——翻页数据命中率低，缓存收益不抵存储与失效成本。
     */
    fun page(module: String, vararg params: Pair<String, Any?>) =
        buildString {
            append(V).append(':').append(module)
            params.forEach { (k, v) ->
                append(':').append(k).append('=').append(v ?: "all")
            }
        }

    /** 详情。不同业务各自传主键 */
    fun detail(module: String, id: Long) = "$V:detail:$module:$id"

    /**
     * 配置类单项。主键是字符串（如 `app_config`、`checkin_rule`）。
     *
     * 与 [detail] 分开是因为主键类型不同 —— 硬塞进 Long 版会逼调用方
     * 把字符串主键做哈希或映射，反而容易撞键。
     */
    fun config(module: String, name: String) = "$V:config:$module:$name"

    /** 按模块前缀清理：`clearModule("category_tree")` */
    fun modulePrefix(module: String) = "$V:$module"

    /** 清理全部业务缓存时用（去掉版本号前缀即为全量） */
    val all: String get() = V

    // ========= 业务对外方法，保持原有函数签名，上层调用代码无需改动 =========
    /** 分类树。type: 1-文章分类 2-视频课程分类（接口4.1） */
    fun categoryTree(type: Int): String = page("category_tree", "type" to type)

    /**
     * 【任务③：首页课程列表接缓存】
     * 课程列表（接口 6.1）。5 个参数必须全部拼进 key，缺一不可：
     * 缺 pageNum 会串页、缺 categoryId 会串分类、缺 sort 会串排序。
     */
    fun courseHome(
        pageNum: Int,
        pageSize: Int,
        categoryId: Long?,
        videoType: Int?,
        sort: Int?
    ): String = page(
        "course_home",
        "page" to pageNum,
        "size" to pageSize,
        "cat" to categoryId,
        "vtype" to videoType,
        "sort" to sort
    )

    /**
     * 【任务④：课程详情接缓存 + 失效逻辑（过期重取、下拉强制回源）】
     * 课程详情（接口 6.2），按课程 id 区分，一门课一条缓存。
     */
    fun courseDetail(courseId: Long): String = detail("course", courseId)
}
