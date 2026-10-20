package com.github.learningplatform.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.learningplatform.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

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
    }

    val token: Flow<String> = context.dataStore.data.map { it[Keys.TOKEN] ?: "" }
    val userId: Flow<String> = context.dataStore.data.map { it[Keys.USER_ID] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "" }
    val nickname: Flow<String> = context.dataStore.data.map { it[Keys.NICKNAME] ?: "" }
    val avatar: Flow<String> = context.dataStore.data.map { it[Keys.AVATAR] ?: "" }
    val phone: Flow<String> = context.dataStore.data.map { it[Keys.PHONE] ?: "" }

    /** 登录态判断：有 token 即视为已登录 */
    val isLoggedIn: Flow<Boolean> = token.map { it.isNotBlank() }

    /** 主题模式：light / dark，默认浅色 */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.LIGHT
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
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
        avatar: String = ""
    ) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
            it[Keys.USER_NAME] = userName
            it[Keys.PHONE] = phone
            it[Keys.AVATAR] = avatar
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
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
