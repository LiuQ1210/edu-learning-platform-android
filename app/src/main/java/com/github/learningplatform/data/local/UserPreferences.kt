package com.github.learningplatform.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.learningplatform.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局 Preferences DataStore。
 *
 * 任务中心进度也放在同一个文件里：退出登录 / 改密时 [UserPreferences.clear]
 * 会一并清掉任务进度，避免切换账号后继承上一个账号的视频 / 文章计分记录。
 */
internal val Context.appDataStore: DataStore<Preferences> by
    preferencesDataStore(name = Constants.DATASTORE_NAME)

private val exchangeRecordJson = Json { ignoreUnknownKeys = true }

private const val MAX_EXCHANGE_RECORDS = 50

/**
 * 本地登录态（v1 Token 版）。
 *
 * 后端用 Bearer Token 认证，DataStore 存 token + 用户基本信息。
 */
@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
        val PHONE = stringPreferencesKey("phone")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHIP_RECEIVER = stringPreferencesKey("ship_receiver")
        val SHIP_PHONE = stringPreferencesKey("ship_phone")
        val SHIP_REGION = stringPreferencesKey("ship_region")
        val SHIP_DETAIL = stringPreferencesKey("ship_detail")
        val EXCHANGE_RECORDS = stringPreferencesKey("exchange_records")
        val EXCHANGE_RECORDS_SEEDED = booleanPreferencesKey("exchange_records_seeded")
    }

    val token: Flow<String> = context.appDataStore.data.map { it[Keys.TOKEN] ?: "" }
    val userId: Flow<String> = context.appDataStore.data.map { it[Keys.USER_ID] ?: "" }
    val userName: Flow<String> = context.appDataStore.data.map { it[Keys.USER_NAME] ?: "" }
    val nickname: Flow<String> = context.appDataStore.data.map { it[Keys.NICKNAME] ?: "" }
    val avatar: Flow<String> = context.appDataStore.data.map { it[Keys.AVATAR] ?: "" }
    val phone: Flow<String> = context.appDataStore.data.map { it[Keys.PHONE] ?: "" }

    /** 登录态判断：有 token 即视为已登录 */
    val isLoggedIn: Flow<Boolean> = token.map { it.isNotBlank() }

    /** 主题模式：只有浅色/深色两态，未设置时默认浅色，不跟随系统 */
    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.LIGHT
        }
    }

    /** 兑换奖品用的收货信息 */
    val shippingAddress: Flow<ShippingAddress> = context.appDataStore.data.map { prefs ->
        ShippingAddress(
            receiver = prefs[Keys.SHIP_RECEIVER].orEmpty(),
            phone = prefs[Keys.SHIP_PHONE].orEmpty(),
            region = prefs[Keys.SHIP_REGION].orEmpty(),
            detail = prefs[Keys.SHIP_DETAIL].orEmpty()
        )
    }

    /** 兑换记录：最新的排在最前 */
    val exchangeRecords: Flow<List<ExchangeRecord>> = context.appDataStore.data.map { prefs ->
        decodeExchangeRecords(prefs[Keys.EXCHANGE_RECORDS])
    }

    suspend fun addExchangeRecord(record: ExchangeRecord) {
        context.appDataStore.edit { prefs ->
            val current = decodeExchangeRecords(prefs[Keys.EXCHANGE_RECORDS])
            prefs[Keys.EXCHANGE_RECORDS] = exchangeRecordJson.encodeToString(
                (listOf(record) + current).take(MAX_EXCHANGE_RECORDS)
            )
        }
    }

    /** 首次进入兑换记录页时写入几条演示数据，方便查看列表样式 */
    suspend fun seedExchangeRecordsIfNeeded(records: List<ExchangeRecord>) {
        context.appDataStore.edit { prefs ->
            if (prefs[Keys.EXCHANGE_RECORDS_SEEDED] == true) return@edit
            if (prefs[Keys.EXCHANGE_RECORDS].isNullOrBlank()) {
                prefs[Keys.EXCHANGE_RECORDS] = exchangeRecordJson.encodeToString(records)
            }
            prefs[Keys.EXCHANGE_RECORDS_SEEDED] = true
        }
    }

    private fun decodeExchangeRecords(raw: String?): List<ExchangeRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            exchangeRecordJson.decodeFromString<List<ExchangeRecord>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun saveShippingAddress(address: ShippingAddress) {
        context.appDataStore.edit {
            it[Keys.SHIP_RECEIVER] = address.receiver
            it[Keys.SHIP_PHONE] = address.phone
            it[Keys.SHIP_REGION] = address.region
            it[Keys.SHIP_DETAIL] = address.detail
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit {
            it[Keys.THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    /** 一次性读取 token（供拦截器同步调用） */
    suspend fun getToken(): String = token.first()

    /** 一次性读取 userId */
    suspend fun currentUserId(): String = userId.first()

    /** 登录成功后写入 token + 用户信息 */
    suspend fun saveLoginInfo(
        token: String,
        userId: String,
        userName: String,
        phone: String,
        avatar: String = "",
        nickname: String = ""
    ) {
        context.appDataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
            it[Keys.USER_NAME] = userName
            it[Keys.PHONE] = phone
            it[Keys.AVATAR] = avatar
            // 昵称单独存一份，个人主页首屏缓存与后端资料保持一致
            if (nickname.isNotBlank()) it[Keys.NICKNAME] = nickname
        }
    }

    suspend fun clear() {
        context.appDataStore.edit { it.clear() }
    }
}

/** 主题模式：跟随系统 / 强制浅色 / 强制深色 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun label(): String = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }
}
