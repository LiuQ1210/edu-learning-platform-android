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
    val rememberMe: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, error = null)
    }

    fun onRememberMeChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
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

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                authRepository.login(state.username, state.password)
                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
            } catch (e: CancellationException) {
                // 协程取消必须向上抛，不能被 catch 吞掉，否则会破坏结构化并发
                throw e
            } catch (e: Throwable) {
                val msg = if (e is ApiException) e.message else "网络异常，请稍后重试"
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
            }
        }
    }
}