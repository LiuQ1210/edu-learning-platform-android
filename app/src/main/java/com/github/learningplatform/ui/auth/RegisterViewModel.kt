package com.github.learningplatform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreed: Boolean = false,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val registerSuccess: Boolean = false,
    // ---- 图形验证码（图片 base64，后端不校验，仅展示）----
    val captchaImg: String = "",
    val captchaLoading: Boolean = false,
    val captchaError: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        loadCaptcha()
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmError = null, error = null)
    }

    fun onAgreedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(agreed = value, error = null)
    }

    /** 加载图形验证码图片（JPEG 流转 base64） */
    fun loadCaptcha() {
        if (_uiState.value.captchaLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(captchaLoading = true, captchaError = null)
            try {
                val img = authRepository.getCaptchaImage()
                _uiState.value = _uiState.value.copy(captchaImg = img, captchaLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    captchaLoading = false,
                    captchaError = if (e is ApiException) e.message else "验证码加载失败，点击图片重试"
                )
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        when {
            state.username.isBlank() -> {
                _uiState.value = state.copy(usernameError = "请输入手机号")
                return
            }
            state.password.length < 6 -> {
                _uiState.value = state.copy(passwordError = "密码至少 6 位")
                return
            }
            state.password != state.confirmPassword -> {
                _uiState.value = state.copy(confirmError = "两次输入的密码不一致")
                return
            }
            !state.agreed -> {
                _uiState.value = state.copy(error = "请先阅读并同意服务条款")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                authRepository.register(
                    phone = state.username.trim(),
                    password = state.password,
                    confirmPassword = state.confirmPassword
                )
                _uiState.value = _uiState.value.copy(isLoading = false, registerSuccess = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (e is ApiException) e.message else "网络异常，请稍后重试"
                )
                loadCaptcha()
            }
        }
    }
}
