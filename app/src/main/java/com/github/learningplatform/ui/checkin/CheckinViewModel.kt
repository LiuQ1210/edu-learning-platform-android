package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.AdFreeRewardDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.remote.dto.PointsAccountDto
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailySignUiState(
    val isLoading: Boolean = false,
    val status: DailySignStatusDto? = null,
    /** 积分账户：余额 + 免广告权益（服务端算好，客户端不本地推算） */
    val account: PointsAccountDto? = null,
    val rewards: List<AdFreeRewardDto> = emptyList(),
    val message: String? = null,
    val signing: Boolean = false,
    val exchanging: Boolean = false
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailySignUiState())
    val uiState: StateFlow<DailySignUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val status = checkinRepository.getStatus()
                val account = checkinRepository.getAccount()
                // 档位以后端为准；失败时用兜底列表，避免整页空白
                val rewards = runCatching { checkinRepository.getExchangeTiers() }
                    .getOrDefault(emptyList())
                    .ifEmpty { com.github.learningplatform.data.remote.dto.ExchangeResultDto.OPTIONS }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = status,
                    account = account,
                    rewards = rewards
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = e.readableMessage()
                )
            }
        }
    }

    fun sign() {
        if (_uiState.value.signing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(signing = true, message = null)
            try {
                val result = checkinRepository.sign()
                _uiState.value = _uiState.value.copy(
                    signing = false,
                    message = result.message,
                    status = checkinRepository.getStatus(),
                    account = checkinRepository.getAccount()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(signing = false, message = e.readableMessage())
            }
        }
    }

    fun exchange(reward: AdFreeRewardDto) {
        if (_uiState.value.exchanging) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exchanging = true, message = null)
            try {
                val result = checkinRepository.exchange(reward)
                _uiState.value = _uiState.value.copy(
                    exchanging = false,
                    message = result.message,
                    status = checkinRepository.getStatus(),
                    account = checkinRepository.getAccount()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(exchanging = false, message = e.readableMessage())
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
