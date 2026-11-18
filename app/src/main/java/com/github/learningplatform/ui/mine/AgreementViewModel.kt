package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.repository.AgreementRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgreementUiState(
    val loading: Boolean = true,
    val title: String = "",
    val contentHtml: String = "",
    val error: String? = null
)

/**
 * 协议详情 ViewModel：按类型拉取后端协议正文（HTML）。
 * 类型：user_agreement / privacy_policy / about_us。
 */
@HiltViewModel
class AgreementViewModel @Inject constructor(
    private val agreementRepository: AgreementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgreementUiState())
    val uiState: StateFlow<AgreementUiState> = _uiState.asStateFlow()

    fun load(agreementType: String) {
        viewModelScope.launch {
            _uiState.value = AgreementUiState(loading = true)
            try {
                val dto = agreementRepository.getAgreement(agreementType)
                _uiState.value = AgreementUiState(
                    loading = false,
                    title = dto.title.ifBlank { dto.typeLabel },
                    contentHtml = dto.content
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = AgreementUiState(
                    loading = false,
                    error = e.readableMessage()
                )
            }
        }
    }
}
