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

    /** 分类树：24 小时（分类结构低频变化，可放宽） */
    const val CATEGORY_TREE = 24 * 60 * 60 * 1000L

    /** 首页课程列表：10 分钟（课程热度/上新较活跃，比分类树短）【任务③】 */
    const val COURSE_HOME = 10 * 60 * 1000L

    /** 课程详情：1 小时（详情内容基本稳定）【任务④】 */
    const val COURSE_DETAIL = 60 * 60 * 1000L
}
