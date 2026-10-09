package com.github.learningplatform.data.local

import com.github.learningplatform.core.network.AppJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存时长分档。
 *
 * 按「数据多久会变一次」分档，而不是每个接口自己拍一个数 ——
 * 后者很快会变成一堆 30 分钟 / 1 小时 / 2 小时的随机值，没人说得清依据。
 *
 * 选定依据：**一律短于后端的缓存时长**。
 * 后端用 Redis 缓存数据时，客户端如果缓存更久，用户会看到明显过期的内容。
 * 拿不准就选短的一档，缓存命中的价值远小于显示错数据的代价。
 */
object CacheTtl {
    /** 几乎不变：分类树、字典、榜单配置 */
    const val STATIC = 6 * 60 * 60 * 1000L

    /** 变动慢：课程详情、文章详情 */
    const val LOOSE = 30 * 60 * 1000L

    /** 变动中等：首页列表、搜索结果 */
    const val MEDIUM = 5 * 60 * 1000L

    /** 变动快：评论、签到状态、个人统计 */
    const val TIGHT = 30 * 1000L

    /**
     * 陈旧条目的保留上限：超过就清掉。
     * 比最长的 TTL 长得多，因为过期缓存还有「断网兜底」的用处。
     */
    const val MAX_AGE = 8 * 24 * 60 * 60 * 1000L
}

/**
 * 业务缓存读写。
 *
 * 职责只有「存」和「取」，数据来源决策在 [cached] 里。
 *
 * 为什么值用 JSON 字符串存 KV 表而不是逐模块建表：
 * 缓存的是整个接口响应（列表、树），形态差异大，逐模块建表开发和迁移成本都高。
 * KV 牺牲了按字段查询的能力，但本项目缓存只做「整取整存」，不需要查询。
 * 真正需要按字段查的业务（观看历史等）已在数据库设计里单独建表。
 */
@Singleton
class CacheStore @Inject constructor(
    private val dao: CacheDao
) {

    /**
     * 读缓存。
     *
     * @param ttlMillis 新鲜度上限，用 [CacheTtl] 的档位。传 0 表示不判断过期
     * @return 缓存存在且未过期时返回值，否则 null
     */
    suspend fun <T> get(key: String, ttlMillis: Long, serializer: KSerializer<T>): T? {
        val entry = readEntry(key) ?: return null
        if (isExpired(entry.updatedAt, ttlMillis)) return null
        return decode(key, entry.value, serializer)
    }

    /** 读缓存并忽略新鲜度（网络失败时的旧数据回退用） */
    suspend fun <T> getStale(key: String, serializer: KSerializer<T>): T? {
        val entry = readEntry(key) ?: return null
        return decode(key, entry.value, serializer)
    }

    suspend fun <T> put(key: String, value: T, serializer: KSerializer<T>) {
        runCatching {
            dao.put(CacheEntry(key = key, value = AppJson.encodeToString(serializer, value)))
        }.onFailure {
            // 写失败不能影响主流程：顶多下次重新请求
            Timber.w(it, "缓存写入失败 key=%s", key)
        }
    }

    suspend fun remove(key: String) = dao.remove(key)

    /** 按模块前缀失效，如发布文章后清掉文章列表 */
    suspend fun invalidateModule(module: String) = dao.removeByPrefix(CacheKeys.modulePrefix(module))

    suspend fun clearAll() = dao.clear()

    /** 清理陈旧条目，避免 KV 表无限增长 */
    suspend fun prune() = dao.removeOlderThan(System.currentTimeMillis() - CacheTtl.MAX_AGE)

    // ---------------------------------------------------------------- 内部

    /** internal use：公开的 inline 读取函数需要访问它 */
    suspend fun readEntry(key: String): CacheEntry? =
        runCatching { dao.getEntry(key) }
            .onFailure { Timber.w(it, "缓存读取失败 key=%s", key) }
            .getOrNull()

    fun isExpired(updatedAt: Long, ttlMillis: Long): Boolean =
        ttlMillis > 0L && System.currentTimeMillis() - updatedAt > ttlMillis

    suspend fun <T> decode(key: String, text: String, serializer: KSerializer<T>): T? =
        runCatching { AppJson.decodeFromString(serializer, text) }
            .onFailure {
                // 解码失败通常意味着模型变了（字段改名/删除），旧缓存已无价值。
                // 不删的话每次读都会失败，等于这块缓存永久失效。
                Timber.w(it, "缓存解码失败，已清除 key=%s", key)
                dao.remove(key)
            }
            .getOrNull()
}

// ---------------------------------------------------------------------------
// 便捷读取：把 serializer 推导出来，调用方少写一坨样板
//
// 这几个是 inline + reified，所以只能调到 CacheStore 的公开成员（见上面标注）。
// ---------------------------------------------------------------------------

/** 读列表缓存：`cache.getList<CategoryNodeDto>(key, ttl)` */
suspend inline fun <reified T> CacheStore.getList(key: String, ttlMillis: Long): List<T>? =
    get(key, ttlMillis, ListSerializer(serializer<T>()))

/** 读单个对象缓存 */
suspend inline fun <reified T> CacheStore.getObject(key: String, ttlMillis: Long): T? =
    get(key, ttlMillis, serializer<T>())

/** 写列表缓存 */
suspend inline fun <reified T> CacheStore.putList(key: String, value: List<T>) =
    put(key, value, ListSerializer(serializer<T>()))

/** 写单个对象缓存 */
suspend inline fun <reified T> CacheStore.putObject(key: String, value: T) =
    put(key, value, serializer<T>())

/** 网络失败时的旧列表回退 */
suspend inline fun <reified T> CacheStore.getStaleList(key: String): List<T>? =
    getStale(key, ListSerializer(serializer<T>()))

/** 网络失败时的旧对象回退 */
suspend inline fun <reified T> CacheStore.getStaleObject(key: String): T? =
    getStale(key, serializer<T>())

/**
 * 缓存优先读取：新鲜则直接返回，否则回源；回源失败时退回过期缓存。
 *
 * 三种结果：
 *  1. 缓存新鲜          → 返回缓存，不发请求
 *  2. 缓存缺失/过期      → 发请求；成功后写缓存并返回
 *  3. 请求失败但有旧缓存  → **返回旧缓存**，不抛异常
 *
 * 第 3 条是刻意设计：弱网或断网时，用户看到略旧的内容比看到错误页好得多。
 * 代价是数据可能不最新 —— 可接受，因为 TTL 本身就不长。
 *
 * **不能接受旧数据的业务不要用这个函数**（如支付结果、签到结果），直接调仓储。
 */
suspend inline fun <reified T> CacheStore.cachedList(
    key: String,
    ttlMillis: Long,
    crossinline fetch: suspend () -> List<T>
): List<T> {
    getList<T>(key, ttlMillis)?.let { return it }
    return try {
        val fresh = fetch()
        putList(key, fresh)
        fresh
    } catch (e: Throwable) {
        val stale = getStaleList<T>(key)
        if (stale != null) {
            Timber.w(e, "回源失败，使用过期缓存 key=%s", key)
            stale
        } else {
            throw e
        }
    }
}

/** 对象版的 [cachedList] */
suspend inline fun <reified T> CacheStore.cachedObject(
    key: String,
    ttlMillis: Long,
    crossinline fetch: suspend () -> T
): T {
    getObject<T>(key, ttlMillis)?.let { return it }
    return try {
        val fresh = fetch()
        putObject(key, fresh)
        fresh
    } catch (e: Throwable) {
        val stale = getStaleObject<T>(key)
        if (stale != null) {
            Timber.w(e, "回源失败，使用过期缓存 key=%s", key)
            stale
        } else {
            throw e
        }
    }
}
