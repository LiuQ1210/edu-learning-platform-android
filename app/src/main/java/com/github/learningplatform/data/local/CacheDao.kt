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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_kv WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM cache_kv WHERE updatedAt < :before")
    suspend fun removeOlderThan(before: Long)

    @Query("DELETE FROM cache_kv")
    suspend fun clear()
}