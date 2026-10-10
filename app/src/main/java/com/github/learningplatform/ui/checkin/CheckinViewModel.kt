package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.AdFreeRewardDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.repository.CheckinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailySignUiState(
    val isLoading: Boolean = false,
    val status: DailySignStatusDto? = null,
    val rewards: List<AdFreeRewardDto> = emptyList(),
    val message: String? = null,
    val signing: Boolean = false
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
            val status = checkinRepository.getStatus()
            val rewards = checkinRepository.rewards()
            _uiState.value = _uiState.value.copy(isLoading = false, status = status, rewards = rewards)
        }
    }

    fun sign() {
        if (_uiState.value.signing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(signing = true, message = null)
            val result = checkinRepository.sign()
            _uiState.value = _uiState.value.copy(
                signing = false,
                message = result.message,
                status = checkinRepository.getStatus()
            )
        }
    }

    fun exchange(reward: AdFreeRewardDto) {
        viewModelScope.launch {
            val result = checkinRepository.exchange(reward)
            _uiState.value = _uiState.value.copy(
                message = result.message,
                status = checkinRepository.getStatus()
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
