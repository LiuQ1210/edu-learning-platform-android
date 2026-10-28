package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.AdFreeRewardDto
import com.github.learningplatform.data.remote.dto.CheckinRulesDto
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PointsRulesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** `/api/v1/checkin/rules`：签到档位 + 循环/重置说明 */
    val rules: CheckinRulesDto? = null,
    /** `/api/v1/points/exchange/tiers`：兑换档位 */
    val rewards: List<AdFreeRewardDto> = emptyList()
)

@HiltViewModel
class PointsRulesViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PointsRulesUiState(isLoading = true))
    val uiState: StateFlow<PointsRulesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val rules = checkinRepository.getRules()
                val rewards = runCatching { checkinRepository.getExchangeTiers() }
                    .getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    rules = rules,
                    rewards = rewards
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.readableMessage()
                )
            }
        }
    }
}
