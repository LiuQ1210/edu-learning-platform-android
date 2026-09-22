package com.github.learningplatform.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 通用键值缓存表。
 *
 * 路线图约定「Room 存业务缓存」，这里先提供一张通用的 KV 表，
 * 供后续列表缓存 / 接口结果缓存复用，避免每接一个模块就建一张表。
 * 需要结构化查询的业务（如观看历史）再单独建实体。
 */
@Entity(tableName = "cache_kv")
data class CacheEntry(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)