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
 * 本地登录态。
 *
 * v3.0 为双 Token 机制：accessToken（2h）+ refreshToken（7d，轮转）。
 */
@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
    }

    val accessToken: Flow<String> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] ?: "" }
    val refreshToken: Flow<String> = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] ?: "" }
    val userId: Flow<String> = context.dataStore.data.map { it[Keys.USER_ID] ?: "" }
    val nickname: Flow<String> = context.dataStore.data.map { it[Keys.NICKNAME] ?: "" }
    val avatar: Flow<String> = context.dataStore.data.map { it[Keys.AVATAR] ?: "" }
    val isLoggedIn: Flow<Boolean> = accessToken.map { it.isNotBlank() }

    /** 一次性读取（不要用 collect，DataStore.data 是永不完成的 Flow） */
    suspend fun currentAccessToken(): String = accessToken.first()

    suspend fun currentRefreshToken(): String = refreshToken.first()

    /** 登录 / 注册成功后写入 */
    suspend fun saveLoginInfo(
        accessToken: String,
        refreshToken: String,
        userId: String,
        nickname: String,
        avatar: String
    ) {
        context.dataStore.edit {
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
            it[Keys.USER_ID] = userId
            it[Keys.NICKNAME] = nickname
            it[Keys.AVATAR] = avatar
        }
    }

    /** 刷新成功后轮转两个 Token */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}