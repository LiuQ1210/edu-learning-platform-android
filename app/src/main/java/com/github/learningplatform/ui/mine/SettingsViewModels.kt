package com.github.learningplatform.ui.mine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.ThemeMode
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.repository.AuthRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val loggedOut: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cacheSizeLabel: String = "计算中...",
    val cleared: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 把 DataStore 里的主题模式接到 UI 状态；用 stateIn 让设置页一打开就有当前值
        viewModelScope.launch {
            userPreferences.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        computeCacheSize()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // AuthRepository.logout 一定会清空本地登录态
            }
            _uiState.value = SettingsUiState(loggedOut = true)
        }
    }

    /** 递归算 cache 目录总大小，格式化成 KB/MB 字符串 */
    private fun computeCacheSize() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) { dirSize(context.cacheDir) }
            _uiState.value = _uiState.value.copy(cacheSizeLabel = formatSize(size))
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            _uiState.value = _uiState.value.copy(cacheSizeLabel = "0 B", cleared = true)
        }
    }

    fun consumeCleared() { _uiState.value = _uiState.value.copy(cleared = false) }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += if (child.isDirectory) dirSize(child) else child.length()
        }
        return total
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }
}

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onOldPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(oldPassword = value, error = null)
    }

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, error = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.submitting) return

        // 客户端先做本地一致性校验，减少一次失败请求
        val localError = when {
            state.oldPassword.isBlank() -> "请输入当前密码"
            state.newPassword.length < 6 -> "新密码至少 6 位"
            state.newPassword == state.oldPassword -> "新密码不能与当前密码相同"
            state.newPassword != state.confirmPassword -> "两次输入的新密码不一致"
            else -> null
        }
        if (localError != null) {
            _uiState.value = state.copy(error = localError)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(submitting = true, error = null)
            try {
                authRepository.changePassword(
                    oldPassword = state.oldPassword,
                    newPassword = state.newPassword,
                    confirmPassword = state.confirmPassword
                )
                // 后端已吊销全部 Token，本地登录态也已被清空 -> 交给 UI 回登录页
                _uiState.value = _uiState.value.copy(submitting = false, done = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    submitting = false,
                    error = e.readableMessage()
                )
            }
        }
    }
}