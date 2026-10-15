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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val rememberMe: Boolean = false,
    // ---- 图形验证码（图片 base64，后端不校验，仅展示）----
    val captchaImg: String = "",
    val captchaLoading: Boolean = false,
    val captchaError: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadCaptcha()
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, error = null)
    }

    fun onRememberMeChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
    }

    /** 找回密码（暂未开放） */
    fun notifyForgotPassword() {
        _uiState.value = _uiState.value.copy(error = "找回密码暂未开放，请联系管理员")
    }

    /** 加载图形验证码图片（JPEG 流转 base64，点击图片刷新） */
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

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.username.isBlank()) {
            _uiState.value = state.copy(usernameError = "请输入手机号")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(passwordError = "请输入密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                authRepository.login(state.username, state.password)
                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val msg = if (e is ApiException) e.message else "网络异常，请稍后重试"
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
                loadCaptcha()
            }
        }
    }
}
