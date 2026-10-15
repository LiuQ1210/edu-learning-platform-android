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
 * 本地登录态（Shiro Session 版）。
 *
 * 后端用 JSESSIONID Cookie 认证（OkHttp CookieJar 内存管理），
 * DataStore 只存用户基本信息用于 UI 展示和登录态判断。
 */
@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
        val PHONE = stringPreferencesKey("phone")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userId: Flow<String> = context.dataStore.data.map { it[Keys.USER_ID] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "" }
    val nickname: Flow<String> = context.dataStore.data.map { it[Keys.NICKNAME] ?: "" }
    val avatar: Flow<String> = context.dataStore.data.map { it[Keys.AVATAR] ?: "" }
    val phone: Flow<String> = context.dataStore.data.map { it[Keys.PHONE] ?: "" }

    /** 登录态判断：有 userId 即视为已登录（Session Cookie 由 CookieJar 内存管理） */
    val isLoggedIn: Flow<Boolean> = userId.map { it.isNotBlank() }

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

    /** 一次性读取 userId */
    suspend fun currentUserId(): String = userId.first()

    /** 登录成功后写入用户信息 */
    suspend fun saveLoginInfo(
        userId: String,
        userName: String,
        nickname: String,
        phone: String,
        avatar: String = ""
    ) {
        context.dataStore.edit {
            it[Keys.USER_ID] = userId
            it[Keys.USER_NAME] = userName
            it[Keys.NICKNAME] = nickname
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
