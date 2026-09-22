package com.github.learningplatform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.dto.RegisterRequest
import com.github.learningplatform.data.repository.AuthRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")

/** 验证码限发间隔：接口 2.1 规定 60 秒内限发一次 */
private const val CODE_COUNTDOWN_SECONDS = 60

/** 注册通道（v3.1 增补 2.1：accountType 1-手机号 2-邮箱） */
enum class RegisterChannel(val accountType: Int, val label: String) {
    PHONE(RegisterRequest.ACCOUNT_TYPE_PHONE, "手机号"),
    EMAIL(RegisterRequest.ACCOUNT_TYPE_EMAIL, "邮箱")
}

data class RegisterUiState(
    val channel: RegisterChannel = RegisterChannel.EMAIL,
    /** 手机号或邮箱，按 channel 解释 */
    val account: String = "",
    val code: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreed: Boolean = false,
    val isLoading: Boolean = false,
    val sendingCode: Boolean = false,
    /** 验证码倒计时剩余秒数，0 表示可发送 */
    val countdown: Int = 0,
    val accountError: String? = null,
    val codeError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val message: String? = null,
    val registerSuccess: Boolean = false
)

/**
 * 注册（接口文档 v3.1 增补 2.2：手机号 / 邮箱双通道）。
 *
 * 三步流程：
 *  1. `POST auth/send-code`（target=account，targetType 按通道，scene=register）
 *  2. 用户填写收到的 6 位验证码
 *  3. `POST auth/register`（account + accountType + password + code）
 *
 * 账号本身（手机号或邮箱）就是登录标识：登录时 2.3 的 username 可传手机号或邮箱。
 * 昵称选填，后端不传会自动生成。
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /** 切换注册通道：清掉账号与验证码（两者的格式与通道绑定） */
    fun onChannelChange(channel: RegisterChannel) {
        if (channel == _uiState.value.channel) return
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            channel = channel,
            account = "",
            code = "",
            accountError = null,
            codeError = null,
            error = null,
            countdown = 0
        )
    }

    fun onAccountChange(value: String) {
        _uiState.value = _uiState.value.copy(account = value, accountError = null, error = null)
    }

    fun onCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(code = value, codeError = null, error = null)
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

    fun onAgreedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(agreed = value, error = null)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** 发送验证码：邮箱走邮件通道，手机号走短信通道（接口 2.1） */
    fun sendCode() {
        val state = _uiState.value
        if (state.sendingCode || state.countdown > 0) return

        val accountError = validateAccount(state.account, state.channel)
        if (accountError != null) {
            _uiState.value = state.copy(accountError = accountError)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(sendingCode = true, error = null)
            try {
                val expire = authRepository.sendCode(
                    target = state.account.trim(),
                    targetType = state.channel.accountType,
                    scene = SCENE_REGISTER
                )
                _uiState.value = _uiState.value.copy(
                    sendingCode = false,
                    countdown = CODE_COUNTDOWN_SECONDS,
                    message = if (expire in 1..600) {
                        "验证码已发送，${expire / 60} 分钟内有效"
                    } else {
                        "验证码已发送，请查收"
                    }
                )
                startCountdown()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    sendingCode = false,
                    error = e.readableMessage()
                )
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdown > 0) {
                delay(1_000)
                _uiState.value = _uiState.value.copy(countdown = _uiState.value.countdown - 1)
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        // 逐字段校验，错误各自绑定到对应输入框
        val accountError = validateAccount(state.account, state.channel)
        when {
            accountError != null -> {
                _uiState.value = state.copy(accountError = accountError)
                return
            }
            state.code.isBlank() -> {
                _uiState.value = state.copy(codeError = "请输入验证码")
                return
            }
            state.code.length != 6 -> {
                _uiState.value = state.copy(codeError = "验证码为 6 位数字")
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
                    account = state.account.trim(),
                    password = state.password,
                    code = state.code.trim(),
                    accountType = state.channel.accountType,
                    nickname = state.nickname.trim().ifBlank { null }
                )
                _uiState.value = _uiState.value.copy(isLoading = false, registerSuccess = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 1008/1009 是「已被注册」：绑到账号输入框比丢一句通用错误更直观
                val code = (e as? ApiException)?.code
                val bound = code == Constants.CODE_PHONE_BOUND || code == Constants.CODE_EMAIL_BOUND
                val captchaWrong = code == Constants.CODE_CAPTCHA_ERROR
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    accountError = if (bound) "${state.channel.label}已被注册，请直接登录" else null,
                    codeError = if (captchaWrong) "验证码错误" else null,
                    error = if (bound || captchaWrong) null else e.readableMessage()
                )
            }
        }
    }

    private fun validateAccount(account: String, channel: RegisterChannel): String? {
        val value = account.trim()
        if (value.isEmpty()) return "请输入${channel.label}"
        return when (channel) {
            RegisterChannel.PHONE ->
                if (PHONE_REGEX.matches(value)) null else "请输入正确的手机号"
            RegisterChannel.EMAIL ->
                if (EMAIL_REGEX.matches(value)) null else "请输入正确的邮箱"
        }
    }

    private companion object {
        const val SCENE_REGISTER = "register"
    }
}
