package com.github.learningplatform.data.local

/**
 * 【任务②：定缓存键规则与 TTL；分类树接 Room 缓存】
 *
 * 各业务数据的缓存有效期（TTL，单位毫秒）。按「数据变化频率」定值：
 * 变化越慢 TTL 越长，越频繁越短；带时效的一次性数据（如播放凭证）一律不缓存。
 *
 * 过期判断统一在 Repository 侧：
 * `System.currentTimeMillis() - CacheEntry.updatedAt > TTL` 即视为过期，触发回源。
 */
object CacheTtl {

    // ---- 通用分档（CacheStore 体系用）：按「数据多久会变一次」分档 ----
    /** 几乎不变：分类树、字典、榜单配置 */
    const val STATIC = 6 * 60 * 60 * 1000L

    /** 变动慢：课程详情、文章详情 */
    const val LOOSE = 30 * 60 * 1000L

    /** 变动中等：首页列表、搜索结果 */
    const val MEDIUM = 5 * 60 * 1000L

    /** 变动快：评论、签到状态、个人统计 */
    const val TIGHT = 30 * 1000L

    /** 陈旧条目的保留上限：超过就清掉（过期缓存还有断网兜底的用处） */
    const val MAX_AGE = 8 * 24 * 60 * 60 * 1000L

    // ---- 业务精确 TTL（Repository 直连 Room 体系用） ----
    /** 分类树：24 小时（分类结构低频变化，可放宽） */
    const val CATEGORY_TREE = 24 * 60 * 60 * 1000L

    /** 首页课程列表：10 分钟（课程热度/上新较活跃，比分类树短）【任务③】 */
    const val COURSE_HOME = 10 * 60 * 1000L

    /** 课程详情：1 小时（详情内容基本稳定）【任务④】 */
    const val COURSE_DETAIL = 60 * 60 * 1000L
}
