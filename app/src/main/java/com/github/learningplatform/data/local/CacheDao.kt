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

    @Query("SELECT * FROM cache_kv WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<CacheEntry?>

    /**
     * 取整条记录（含 `updatedAt`）。
     *
     * 与 [get] 的区别：TTL 判断需要写入时间，[get] 只返回字符串拿不到时间戳。
     * 缓存读取一律用这个，[get] 保留给不需要判断新鲜度的场景。
     */
    @Query("SELECT * FROM cache_kv WHERE `key` = :key LIMIT 1")
    suspend fun getEntry(key: String): CacheEntry?

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
