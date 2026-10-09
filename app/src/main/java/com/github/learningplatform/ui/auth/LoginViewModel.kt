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
    /** 记住我：勾选后由后端签发更长有效期的 refreshToken（前端仅传递标记） */
    val rememberMe: Boolean = false,
    // ---- 【登录注册联调】若依图形验证码 ----
    val captchaUuid: String = "",
    val captchaImg: String = "",
    val captchaCode: String = "",
    val captchaError: String? = null,
    val captchaLoading: Boolean = false
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

    fun onCaptchaCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(captchaCode = value, captchaError = null, error = null)
    }

    fun onRememberMeChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
    }

    /** 加载若依图形验证码（点击图片可刷新） */
    fun loadCaptcha() {
        if (_uiState.value.captchaLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(captchaLoading = true, captchaError = null)
            try {
                val captcha = authRepository.getCaptchaImage()
                _uiState.value = _uiState.value.copy(
                    captchaUuid = captcha.uuid,
                    captchaImg = captcha.img,
                    captchaLoading = false
                )
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

    /** 找回密码（接口 2.11 需要手机号 + 验证码，暂未开放入口） */
    fun notifyForgotPassword() {
        _uiState.value = _uiState.value.copy(error = "找回密码需要短信验证码，暂未开放，请联系管理员")
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        // 校验错误分别绑定到对应输入框。
        // 原实现把两种错误都挂在密码框上，"请输入用户名"会显示在密码输入框下方。
        if (state.username.isBlank()) {
            _uiState.value = state.copy(usernameError = "请输入用户名")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(passwordError = "请输入密码")
            return
        }
        if (state.captchaCode.isBlank()) {
            _uiState.value = state.copy(captchaError = "请输入验证码")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                authRepository.login(state.username, state.password, state.captchaCode, state.captchaUuid)
                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
            } catch (e: CancellationException) {
                // 协程取消必须向上抛，不能被 catch 吞掉，否则会破坏结构化并发
                throw e
            } catch (e: Throwable) {
                val msg = if (e is ApiException) e.message else "网络异常，请稍后重试"
                // 登录失败（验证码过期/密码错误）后刷新图形验证码，避免用户拿旧验证码重试
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg, captchaCode = "")
                loadCaptcha()
            }
        }
    }
}
