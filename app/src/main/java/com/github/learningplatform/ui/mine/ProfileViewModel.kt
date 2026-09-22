package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.repository.AuthRepository
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val nickname: String = "",
    val avatar: String = "",
    val bio: String = "",
    val continueSignDay: Int = 0,
    val lastSignDate: String = "",
    val unreadCount: Int = 0,
    val error: String? = null,
    val loggedOut: Boolean = false
) {
    /**
     * 今天是否已签到。
     *
     * 不用 java.time：minSdk = 24 且未开启 core library desugaring，
     * Android 7.x 上 java.time.* 会抛 NoClassDefFoundError。
     */
    val signedToday: Boolean
        get() = lastSignDate.isNotBlank() && lastSignDate.startsWith(today())
}

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

/**
 * 个人主页。
 *
 * 昵称/头像优先用 DataStore 缓存渲染（避免首屏空白），再用 3.1 的返回值覆盖。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cachedNickname = runCatching { userPreferences.nickname.first() }.getOrDefault("")
            val cachedAvatar = runCatching { userPreferences.avatar.first() }.getOrDefault("")
            if (cachedNickname.isNotBlank() || cachedAvatar.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    nickname = cachedNickname,
                    avatar = cachedAvatar
                )
            }
        }
        refresh()
    }


    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = userRepository.getProfile()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nickname = profile.nickname.ifBlank { profile.username },
                    avatar = profile.avatar,
                    bio = profile.bio,
                    continueSignDay = profile.continueSignDay,
                    lastSignDate = profile.lastSignDate
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // AuthRepository.logout 内部已兜底清理本地登录态
            }
            _uiState.value = _uiState.value.copy(loggedOut = true)
        }
    }
}
