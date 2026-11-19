package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.repository.StudyRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyStatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: StudyStatsDto? = null
)

/** 学习数据总览（后端 GET /api/v1/learn/stats） */
@HiltViewModel
class StudyStatsViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyStatsUiState())
    val uiState: StateFlow<StudyStatsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = studyRepository.getStats()
                _uiState.value = StudyStatsUiState(isLoading = false, stats = stats)
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
