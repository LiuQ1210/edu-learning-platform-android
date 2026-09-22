package com.github.learningplatform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 本地数据库。
 *
 * 说明：Room 要求 @Database 至少声明一个实体，所以这里挂上通用的 KV 缓存表。
 * 后续每个业务模块（观看历史、笔记、待办等）接入时在此追加实体与版本号。
 */
@Database(
    entities = [CacheEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}