package com.github.learningplatform.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {

    @Query("SELECT value FROM cache_kv WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    /**
     * 【任务②：定缓存键规则与 TTL；分类树接 Room 缓存】
     * 取完整缓存条目（含 updatedAt），供 Repository 做 TTL 过期判断后决定是否回源。
     */
    @Query("SELECT * FROM cache_kv WHERE `key` = :key LIMIT 1")
    suspend fun getEntry(key: String): CacheEntry?

    @Query("SELECT * FROM cache_kv WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<CacheEntry?>

    /**
     * 按前缀删除。
     *
     * 用来做「某个模块的数据全失效」，比如发布文章后清掉文章列表缓存。
     * 注意 `%` 和 `_` 在 LIKE 里是通配符，键名请勿包含这两个字符。
     */
    @Query("DELETE FROM cache_kv WHERE `key` LIKE :prefix || '%'")
    suspend fun removeByPrefix(prefix: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_kv WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM cache_kv WHERE updatedAt < :before")
    suspend fun removeOlderThan(before: Long)

    @Query("DELETE FROM cache_kv")
    suspend fun clear()
}
