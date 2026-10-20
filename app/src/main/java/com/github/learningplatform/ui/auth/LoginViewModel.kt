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

    /** 找回密码（暂未开放） */
    fun notifyForgotPassword() {
        _uiState.value = _uiState.value.copy(error = "找回密码暂未开放，请联系管理员")
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
            }
        }
    }
}
