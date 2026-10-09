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
    /** 若依注册：username 为登录账号（用户名/手机号均可，以后端校验为准） */
    val username: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreed: Boolean = false,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val registerSuccess: Boolean = false,
    // ---- 【登录注册联调】若依图形验证码 ----
    val captchaUuid: String = "",
    val captchaImg: String = "",
    val captchaCode: String = "",
    val captchaError: String? = null,
    val captchaLoading: Boolean = false
)

/**
 * 注册（【登录注册联调】按若依 RegisterDTO 适配）。
 *
 * 若依注册无短信/邮箱验证码通道，改为**图形验证码**流程：
 *  1. GET /captchaImage 拿 uuid + base64 图片
 *  2. 用户看图输入验证码
 *  3. POST /api/user/register（username + password + nickname + code + uuid）
 * 注册成功即返回 token 并写入本地登录态。
 */
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

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmError = null, error = null)
    }

    fun onCaptchaCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(captchaCode = value, captchaError = null, error = null)
    }

    fun onAgreedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(agreed = value, error = null)
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

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        // 逐字段校验，错误各自绑定到对应输入框
        when {
            state.username.isBlank() -> {
                _uiState.value = state.copy(usernameError = "请输入用户名/手机号")
                return
            }
            state.captchaCode.isBlank() -> {
                _uiState.value = state.copy(captchaError = "请输入验证码")
                return
            }
            state.password.length < 6 -> {
                _uiState.value = state.copy(passwordError = "密码至少 6 位")
                return
            }
            !state.password.any { it.isLetter() } || !state.password.any { it.isDigit() } -> {
                _uiState.value = state.copy(passwordError = "密码需同时包含字母和数字")
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
                    username = state.username.trim(),
                    password = state.password,
                    nickname = state.nickname.trim().ifBlank { null },
                    code = state.captchaCode.trim(),
                    uuid = state.captchaUuid
                )
                _uiState.value = _uiState.value.copy(isLoading = false, registerSuccess = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 注册失败（用户名已存在/验证码错误等）后刷新图形验证码，避免旧验证码重试
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (e is ApiException) e.message else "网络异常，请稍后重试",
                    captchaCode = ""
                )
                loadCaptcha()
            }
        }
    }
}
