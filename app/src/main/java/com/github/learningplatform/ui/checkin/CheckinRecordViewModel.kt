package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CheckinCalendarDto
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckinRecordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 三个统计卡与「我的积分」 */
    val status: DailySignStatusDto? = null,
    /** 月历：哪几天签过 */
    val calendar: CheckinCalendarDto? = null,
    /** 最近签到记录 */
    val records: List<CheckinRecordDto> = emptyList()
)

/**
 * 签到记录页（对齐后端 `/checkin/status` + `/checkin/calendar` + `/checkin/records`）。
 *
 * 「今天」用服务端返回的 calendar.today，不用手机本地日期。
 */
@HiltViewModel
class CheckinRecordViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinRecordUiState(isLoading = true))
    val uiState: StateFlow<CheckinRecordUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val status = checkinRepository.getStatus()
                val calendar = checkinRepository.getCalendar()
                val records = checkinRepository.getRecords(pageNum = 1, pageSize = 30).list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = status,
                    calendar = calendar,
                    records = records
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
