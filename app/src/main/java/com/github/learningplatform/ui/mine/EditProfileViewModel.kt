package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.dto.UpdateProfileRequest
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val nickname: String = "",
    val avatar: String = "",
    val gender: Int = 0,
    val bio: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            try {
                val profile = userRepository.getProfile()
                _uiState.value = _uiState.value.copy(
                    nickname = profile.nickname.ifBlank { profile.username },
                    avatar = profile.avatar,
                    gender = profile.gender,
                    bio = profile.bio
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value)
    }

    fun onAvatarChange(value: String) {
        _uiState.value = _uiState.value.copy(avatar = value)
    }

    fun onGenderChange(value: Int) {
        _uiState.value = _uiState.value.copy(gender = value)
    }

    fun onBioChange(value: String) {
        _uiState.value = _uiState.value.copy(bio = value)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        if (state.nickname.isBlank()) {
            _uiState.value = state.copy(message = "昵称不能为空")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(saving = true)
            try {
                userRepository.updateProfile(
                    UpdateProfileRequest(
                        nickname = state.nickname,
                        avatar = state.avatar.ifBlank { null },
                        gender = state.gender,
                        bio = state.bio.ifBlank { null }
                    )
                )
                // 同步本地缓存，个人主页首屏直接用新昵称
                userPreferences.saveLoginInfo(
                    accessToken = userPreferences.currentAccessToken(),
                    refreshToken = userPreferences.currentRefreshToken(),
                    userId = userPreferences.userId.first(),
                    nickname = state.nickname,
                    avatar = state.avatar
                )
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    saved = true,
                    message = "已保存"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(saving = false, message = e.readableMessage())
            }
        }
    }
}
